package com.melissa.diary.domain;

import com.melissa.diary.domain.enums.PaymentEventProcessingStatus;
import com.melissa.diary.domain.enums.PaymentPlatform;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "payment_event",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_event_platform_event", columnNames = {"platform", "event_id"})
        },
        indexes = {
                @Index(name = "idx_payment_event_processing", columnList = "processing_status,received_at"),
                @Index(name = "idx_payment_event_payment", columnList = "payment_id"),
                @Index(name = "idx_payment_event_actor_user", columnList = "actor_user_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentPlatform platform;

    @Column(name = "event_id", nullable = false, length = 200)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actorUser;

    @Column(name = "google_purchase_token_hash", length = 64)
    private String googlePurchaseTokenHash;

    @Column(name = "apple_transaction_id", length = 100)
    private String appleTransactionId;

    @Column(name = "raw_payload", columnDefinition = "JSON")
    private String rawPayload;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 30)
    private PaymentEventProcessingStatus processingStatus;

    @Column(name = "last_error", length = 500)
    private String lastError;
}
