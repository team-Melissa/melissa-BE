package com.melissa.diary.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melissa.diary.config.SqsProperties;
import com.melissa.diary.domain.AsyncJob;
import com.melissa.diary.domain.Diary;
import com.melissa.diary.domain.OutboxEvent;
import com.melissa.diary.domain.enums.AsyncJobStatus;
import com.melissa.diary.domain.enums.AsyncJobType;
import com.melissa.diary.domain.enums.OutboxEventStatus;
import com.melissa.diary.domain.enums.OutboxEventType;
import com.melissa.diary.event.DiaryImageEvent;
import com.melissa.diary.repository.AsyncJobRepository;
import com.melissa.diary.repository.OutboxEventRepository;
import com.melissa.diary.sqs.SqsJobMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryImageDispatchService {

    private static final String TARGET_TYPE_DIARY = "DIARY";

    private final SqsProperties sqsProperties;
    private final AsyncJobRepository asyncJobRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ApplicationEventPublisher publisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public void requestImageGeneration(Diary diary) {
        if (diary == null || diary.getId() == null) {
            throw new IllegalArgumentException("diary must be persisted before image generation dispatch");
        }

        if (!sqsProperties.isEnabled()) {
            publisher.publishEvent(new DiaryImageEvent(diary.getId()));
            return;
        }

        AsyncJob job = asyncJobRepository.findByDedupeKey(buildDedupeKey(diary))
                .orElseGet(() -> asyncJobRepository.save(AsyncJob.builder()
                        .jobType(AsyncJobType.DIARY_IMAGE_GENERATION)
                        .targetType(TARGET_TYPE_DIARY)
                        .targetId(diary.getId())
                        .targetVersion(diary.getVersion())
                        .dedupeKey(buildDedupeKey(diary))
                        .status(AsyncJobStatus.PENDING)
                        .attemptCount(0)
                        .build()));

        if (job.isTerminal()) {
            log.info("[DiaryImageDispatch] terminal job already exists. diaryId={}, jobId={}, status={}",
                    diary.getId(), job.getId(), job.getStatus());
            return;
        }

        OutboxEvent event = outboxEventRepository.save(OutboxEvent.builder()
                .eventType(OutboxEventType.DIARY_IMAGE_REQUESTED)
                .jobId(job.getId())
                .targetId(diary.getId())
                .payloadJson("{}")
                .status(OutboxEventStatus.NEW)
                .publishAttemptCount(0)
                .build());
        event.setPayloadJson(serialize(SqsJobMessage.from(event)));
        outboxEventRepository.save(event);

        log.info("[DiaryImageDispatch] SQS job requested. diaryId={}, version={}, jobId={}, eventId={}",
                diary.getId(), diary.getVersion(), job.getId(), event.getId());
    }

    private String buildDedupeKey(Diary diary) {
        return AsyncJobType.DIARY_IMAGE_GENERATION.name() + ":" + diary.getId() + ":" + diary.getVersion();
    }

    private String serialize(SqsJobMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize SQS job message", e);
        }
    }
}
