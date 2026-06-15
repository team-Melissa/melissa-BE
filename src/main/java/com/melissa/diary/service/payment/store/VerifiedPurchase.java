package com.melissa.diary.service.payment.store;

import com.melissa.diary.domain.enums.PaymentPlatform;
import com.melissa.diary.domain.enums.ProductType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class VerifiedPurchase {
    private PaymentPlatform platform;
    private String storeProductId;
    private ProductType productType;
    private String orderId;
    private String appleTransactionId;
    private String appleOriginalTransactionId;
    private String appleEnvironment;
    private LocalDateTime purchasedAt;
    private LocalDateTime revokedAt;
    private Long amountMicros;
    private String currency;
    private boolean acknowledged;
    private boolean revoked;
    private String rawPayload;
}
