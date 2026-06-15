package com.melissa.diary.service.payment.store;

public record GooglePurchaseVerifyCommand(
        String packageName,
        String productId,
        String purchaseToken,
        String orderId
) {
}
