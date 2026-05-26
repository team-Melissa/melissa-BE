package com.melissa.diary.domain;

import com.melissa.diary.domain.common.BaseEntity;
import com.melissa.diary.domain.enums.OutboxEventStatus;
import com.melissa.diary.domain.enums.OutboxEventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "outbox_event",
        indexes = {
                @Index(name = "idx_outbox_status_next_publish_at", columnList = "status,next_publish_at"),
                @Index(name = "idx_outbox_job_id", columnList = "job_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private OutboxEventType eventType;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private OutboxEventStatus status = OutboxEventStatus.NEW;

    @Column(name = "publish_attempt_count", nullable = false)
    @Builder.Default
    private Integer publishAttemptCount = 0;

    @Column(name = "next_publish_at")
    private LocalDateTime nextPublishAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "sqs_message_id", length = 120)
    private String sqsMessageId;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    public void markPublished(String messageId, LocalDateTime now) {
        this.status = OutboxEventStatus.PUBLISHED;
        this.sqsMessageId = messageId;
        this.publishedAt = now;
        this.lastError = null;
    }

    public void markPublishFailed(String errorMessage, LocalDateTime nextPublishAt) {
        this.status = OutboxEventStatus.FAILED_RETRYABLE;
        this.publishAttemptCount = safePublishAttemptCount() + 1;
        this.nextPublishAt = nextPublishAt;
        this.lastError = truncate(errorMessage);
    }

    private int safePublishAttemptCount() {
        return publishAttemptCount == null ? 0 : publishAttemptCount;
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 500 ? value.substring(0, 500) : value;
    }
}
