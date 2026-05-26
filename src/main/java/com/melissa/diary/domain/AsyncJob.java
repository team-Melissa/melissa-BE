package com.melissa.diary.domain;

import com.melissa.diary.domain.common.BaseEntity;
import com.melissa.diary.domain.enums.AsyncJobStatus;
import com.melissa.diary.domain.enums.AsyncJobType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "async_job",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_async_job_dedupe_key", columnNames = "dedupe_key")
        },
        indexes = {
                @Index(name = "idx_async_job_status", columnList = "status"),
                @Index(name = "idx_async_job_target", columnList = "target_type,target_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsyncJob extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 64)
    private AsyncJobType jobType;

    @Column(name = "target_type", nullable = false, length = 64)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "target_version")
    private Integer targetVersion;

    @Column(name = "dedupe_key", nullable = false, length = 180)
    private String dedupeKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private AsyncJobStatus status = AsyncJobStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    public void markProcessing(LocalDateTime now) {
        this.status = AsyncJobStatus.PROCESSING;
        this.startedAt = now;
        this.completedAt = null;
        this.attemptCount = safeAttemptCount() + 1;
    }

    public void markSucceeded(LocalDateTime now) {
        this.status = AsyncJobStatus.SUCCEEDED;
        this.completedAt = now;
        this.lastError = null;
    }

    public void markCancelled(LocalDateTime now, String reason) {
        this.status = AsyncJobStatus.CANCELLED;
        this.completedAt = now;
        this.lastError = truncate(reason);
    }

    public void markFailedRetryable(String errorMessage) {
        this.status = AsyncJobStatus.FAILED_RETRYABLE;
        this.lastError = truncate(errorMessage);
    }

    public void markFailedFinal(LocalDateTime now, String errorMessage) {
        this.status = AsyncJobStatus.FAILED_FINAL;
        this.completedAt = now;
        this.lastError = truncate(errorMessage);
    }

    public boolean isTerminal() {
        return status == AsyncJobStatus.SUCCEEDED
                || status == AsyncJobStatus.FAILED_FINAL
                || status == AsyncJobStatus.CANCELLED;
    }

    private int safeAttemptCount() {
        return attemptCount == null ? 0 : attemptCount;
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 500 ? value.substring(0, 500) : value;
    }
}
