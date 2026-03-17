package com.melissa.diary.domain;

import com.melissa.diary.domain.enums.IdempotencyStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "idempotency_record",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_idempotency_user_endpoint_key",
                        columnNames = {"user_id", "endpoint", "idempotency_key"}
                )
        },
        indexes = {
                @Index(name = "idx_idempotency_expires_at", columnList = "expires_at")
        }
)
@EntityListeners(AuditingEntityListener.class)
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 120)
    private String endpoint;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IdempotencyStatus status;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    public void markPending() {
        this.status = IdempotencyStatus.PENDING;
        this.responseBody = null;
        this.errorMessage = null;
    }

    public void markSucceeded(String responseBody) {
        this.status = IdempotencyStatus.SUCCEEDED;
        this.responseBody = responseBody;
        this.errorMessage = null;
    }

    public void markFailedRetryable(String errorMessage) {
        this.status = IdempotencyStatus.FAILED_RETRYABLE;
        this.errorMessage = errorMessage;
    }

    public void markFailedFinal(String errorMessage) {
        this.status = IdempotencyStatus.FAILED_FINAL;
        this.errorMessage = errorMessage;
    }
}
