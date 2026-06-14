package com.melissa.diary.domain;

import com.melissa.diary.domain.common.BaseEntity;
import com.melissa.diary.domain.enums.PaymentPlatform;
import com.melissa.diary.domain.enums.PaymentStatus;
import com.melissa.diary.domain.enums.ProductType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "payment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_google_token", columnNames = {"platform", "google_purchase_token_hash"}),
                @UniqueConstraint(name = "uk_payment_apple_transaction", columnNames = {"platform", "apple_transaction_id"})
        },
        indexes = {
                @Index(name = "idx_payment_user_status", columnList = "user_id,status"),
                @Index(name = "idx_payment_user_product", columnList = "user_id,product_id"),
                @Index(name = "idx_payment_google_order", columnList = "google_order_id"),
                @Index(name = "idx_payment_apple_original_transaction", columnList = "apple_original_transaction_id"),
                @Index(name = "idx_payment_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentPlatform platform;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private PaymentProduct product;

    @Column(name = "store_product_id", nullable = false, length = 150)
    private String storeProductId;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 30)
    private ProductType productType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Column(name = "amount_micros")
    private Long amountMicros;

    @Column(length = 10)
    private String currency;

    @Column(name = "google_purchase_token_hash", length = 64)
    private String googlePurchaseTokenHash;

    @Column(name = "google_purchase_token_encrypted", columnDefinition = "TEXT")
    private String googlePurchaseTokenEncrypted;

    @Column(name = "google_order_id", length = 100)
    private String googleOrderId;

    @Column(name = "apple_transaction_id", length = 100)
    private String appleTransactionId;

    @Column(name = "apple_original_transaction_id", length = 100)
    private String appleOriginalTransactionId;

    @Column(name = "apple_environment", length = 30)
    private String appleEnvironment;

    @Column(name = "purchased_at")
    private LocalDateTime purchasedAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "raw_verified_payload", columnDefinition = "JSON")
    private String rawVerifiedPayload;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;
}
