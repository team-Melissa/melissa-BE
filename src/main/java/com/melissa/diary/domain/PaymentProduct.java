package com.melissa.diary.domain;

import com.melissa.diary.domain.common.BaseEntity;
import com.melissa.diary.domain.enums.EntitlementType;
import com.melissa.diary.domain.enums.PaymentPlatform;
import com.melissa.diary.domain.enums.ProductType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "payment_product",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_product_platform_store_product",
                        columnNames = {"platform", "store_product_id"}
                )
        },
        indexes = {
                @Index(name = "idx_payment_product_product_id", columnList = "product_id"),
                @Index(name = "idx_payment_product_active", columnList = "active")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProduct extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, length = 100)
    private String productId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentPlatform platform;

    @Column(name = "store_product_id", nullable = false, length = 150)
    private String storeProductId;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 30)
    private ProductType productType;

    @Enumerated(EnumType.STRING)
    @Column(name = "entitlement_type", length = 50)
    private EntitlementType entitlementType;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;
}
