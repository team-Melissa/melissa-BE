package com.melissa.diary.service.payment.store;

public record ApplePurchaseVerifyCommand(
        String productId,
        String transactionId,
        String originalTransactionId,
        String environment,
        String bundleId
) {
}
