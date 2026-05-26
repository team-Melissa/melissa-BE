package com.melissa.diary.service;

import com.melissa.diary.config.SqsProperties;
import com.melissa.diary.domain.AsyncJob;
import com.melissa.diary.domain.enums.AsyncJobStatus;
import com.melissa.diary.domain.enums.AsyncJobType;
import com.melissa.diary.repository.AsyncJobRepository;
import com.melissa.diary.retry.RetryClassifier;
import com.melissa.diary.sqs.SqsJobMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "melissa.sqs", name = "enabled", havingValue = "true")
public class DiaryImageJobProcessor {

    private final AsyncJobRepository asyncJobRepository;
    private final DiaryImageService diaryImageService;
    private final SqsProperties sqsProperties;
    private final TransactionTemplate transactionTemplate;

    public ProcessResult process(SqsJobMessage message, int receiveCount) {
        PreparedJob preparedJob = prepareJob(message.jobId(), receiveCount);
        if (preparedJob.action() != JobAction.PROCESS) {
            return preparedJob.result();
        }

        try {
            DiaryImageService.JobImageResult result = diaryImageService.generateAndSaveImageForJob(
                    preparedJob.targetId(),
                    preparedJob.targetVersion()
            );

            if (result == DiaryImageService.JobImageResult.CANCELLED) {
                markCancelled(preparedJob.jobId(), "diary image job became stale");
                return ProcessResult.DELETE_MESSAGE;
            }

            markSucceeded(preparedJob.jobId());
            return ProcessResult.DELETE_MESSAGE;
        } catch (RuntimeException e) {
            boolean retryable = RetryClassifier.isRetryable(e);
            boolean exhausted = receiveCount >= sqsProperties.getMaxReceiveCount();

            if (!retryable) {
                markFinalFailure(preparedJob, e);
                return ProcessResult.DELETE_MESSAGE;
            }

            if (exhausted) {
                markFinalFailure(preparedJob, e);
                return ProcessResult.LEAVE_MESSAGE;
            }

            markRetryableFailure(preparedJob.jobId(), e);
            return ProcessResult.LEAVE_MESSAGE;
        }
    }

    private PreparedJob prepareJob(Long jobId, int receiveCount) {
        return transactionTemplate.execute(status -> {
            AsyncJob job = asyncJobRepository.findByIdForUpdate(jobId).orElse(null);
            if (job == null) {
                log.warn("[DiaryImageJob] job not found. jobId={}", jobId);
                return PreparedJob.delete();
            }

            if (job.getStatus() == AsyncJobStatus.SUCCEEDED || job.getStatus() == AsyncJobStatus.CANCELLED) {
                return PreparedJob.delete();
            }

            if (job.getStatus() == AsyncJobStatus.FAILED_FINAL) {
                if (receiveCount >= sqsProperties.getMaxReceiveCount()) {
                    return PreparedJob.leave();
                }
                return PreparedJob.delete();
            }

            if (job.getJobType() != AsyncJobType.DIARY_IMAGE_GENERATION) {
                job.markFailedFinal(LocalDateTime.now(), "unsupported job type: " + job.getJobType());
                return PreparedJob.delete();
            }

            job.markProcessing(LocalDateTime.now());
            return PreparedJob.process(job.getId(), job.getTargetId(), job.getTargetVersion());
        });
    }

    private void markSucceeded(Long jobId) {
        transactionTemplate.executeWithoutResult(status -> {
            AsyncJob job = asyncJobRepository.findByIdForUpdate(jobId).orElse(null);
            if (job == null || job.isTerminal()) {
                return;
            }
            job.markSucceeded(LocalDateTime.now());
        });
    }

    private void markCancelled(Long jobId, String reason) {
        transactionTemplate.executeWithoutResult(status -> {
            AsyncJob job = asyncJobRepository.findByIdForUpdate(jobId).orElse(null);
            if (job == null || job.isTerminal()) {
                return;
            }
            job.markCancelled(LocalDateTime.now(), reason);
        });
    }

    private void markRetryableFailure(Long jobId, RuntimeException exception) {
        transactionTemplate.executeWithoutResult(status -> {
            AsyncJob job = asyncJobRepository.findByIdForUpdate(jobId).orElse(null);
            if (job == null || job.isTerminal()) {
                return;
            }
            job.markFailedRetryable(buildErrorMessage(exception));
        });
    }

    private void markFinalFailure(PreparedJob preparedJob, RuntimeException exception) {
        transactionTemplate.executeWithoutResult(status -> {
            AsyncJob job = asyncJobRepository.findByIdForUpdate(preparedJob.jobId()).orElse(null);
            if (job == null || job.isTerminal()) {
                return;
            }
            job.markFailedFinal(LocalDateTime.now(), buildErrorMessage(exception));
        });
        diaryImageService.markImageFailedForJob(preparedJob.targetId(), preparedJob.targetVersion());
    }

    private String buildErrorMessage(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return "diary image job failed";
        }
        String message = throwable.getMessage();
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    public enum ProcessResult {
        DELETE_MESSAGE,
        LEAVE_MESSAGE
    }

    private enum JobAction {
        PROCESS,
        DELETE,
        LEAVE
    }

    private record PreparedJob(
            JobAction action,
            ProcessResult result,
            Long jobId,
            Long targetId,
            Integer targetVersion
    ) {
        static PreparedJob process(Long jobId, Long targetId, Integer targetVersion) {
            return new PreparedJob(JobAction.PROCESS, null, jobId, targetId, targetVersion);
        }

        static PreparedJob delete() {
            return new PreparedJob(JobAction.DELETE, ProcessResult.DELETE_MESSAGE, null, null, null);
        }

        static PreparedJob leave() {
            return new PreparedJob(JobAction.LEAVE, ProcessResult.LEAVE_MESSAGE, null, null, null);
        }
    }
}
