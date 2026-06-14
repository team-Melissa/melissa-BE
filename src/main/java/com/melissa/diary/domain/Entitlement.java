package com.melissa.diary.domain;

import com.melissa.diary.domain.common.BaseEntity;
import com.melissa.diary.domain.enums.EntitlementSourceType;
import com.melissa.diary.domain.enums.EntitlementType;
import com.melissa.diary.domain.enums.PaymentPlatform;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "entitlement",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_entitlement_user_type", columnNames = {"user_id", "entitlement_type"})
        },
        indexes = {
                @Index(name = "idx_entitlement_active", columnList = "active"),
                @Index(name = "idx_entitlement_source_payment", columnList = "source_payment_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Entitlement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "entitlement_type", nullable = false, length = 50)
    private EntitlementType entitlementType;

    @Column(nullable = false)
    private Boolean active;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private EntitlementSourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_platform", length = 20)
    private PaymentPlatform sourcePlatform;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_payment_id")
    private Payment sourcePayment;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revocation_reason", length = 100)
    private String revocationReason;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;
}
