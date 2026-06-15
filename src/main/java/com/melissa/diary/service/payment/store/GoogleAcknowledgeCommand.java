package com.melissa.diary.service.payment.store;

public record GoogleAcknowledgeCommand(
        String packageName,
        String productId,
        String purchaseToken
) {
}
