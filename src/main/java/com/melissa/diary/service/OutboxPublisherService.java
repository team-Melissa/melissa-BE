package com.melissa.diary.service;

import com.melissa.diary.config.SqsProperties;
import com.melissa.diary.domain.OutboxEvent;
import com.melissa.diary.domain.enums.OutboxEventStatus;
import com.melissa.diary.repository.OutboxEventRepository;
import com.melissa.diary.retry.RetryPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "melissa.sqs", name = "enabled", havingValue = "true")
public class OutboxPublisherService {

    private static final List<OutboxEventStatus> PUBLISHABLE_STATUSES = List.of(
            OutboxEventStatus.NEW,
            OutboxEventStatus.FAILED_RETRYABLE
    );

    private final OutboxEventRepository outboxEventRepository;
    private final SqsClient sqsClient;
    private final SqsProperties sqsProperties;

    public void publishDueEvents() {
        List<OutboxEvent> events = outboxEventRepository.findPublishCandidates(
                PUBLISHABLE_STATUSES,
                LocalDateTime.now(),
                PageRequest.of(0, sqsProperties.getOutboxPublishBatchSize())
        );

        for (OutboxEvent event : events) {
            publish(event);
        }
    }

    private void publish(OutboxEvent event) {
        try {
            SendMessageResponse response = sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(sqsProperties.getJobQueueUrl())
                    .messageBody(event.getPayloadJson())
                    .build());

            event.markPublished(response.messageId(), LocalDateTime.now());
            outboxEventRepository.save(event);
            log.info("[OutboxPublisher] event published. eventId={}, jobId={}, messageId={}",
                    event.getId(), event.getJobId(), response.messageId());
        } catch (RuntimeException e) {
            int nextAttempt = event.getPublishAttemptCount() == null ? 1 : event.getPublishAttemptCount() + 1;
            Duration backoff = RetryPolicy.DIARY_IMAGE.backoffForAttempt(nextAttempt);
            event.markPublishFailed(buildErrorMessage(e), LocalDateTime.now().plus(backoff));
            outboxEventRepository.save(event);
            log.error("[OutboxPublisher] event publish failed. eventId={}, jobId={}, nextAttempt={}, nextDelayMs={}",
                    event.getId(), event.getJobId(), nextAttempt, backoff.toMillis(), e);
        }
    }

    private String buildErrorMessage(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return "SQS publish failed";
        }
        String message = throwable.getMessage();
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
