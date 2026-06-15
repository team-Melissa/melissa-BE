package com.melissa.diary.service.payment.store;

public interface AppleStorePurchaseVerifier {

    VerifiedPurchase verify(ApplePurchaseVerifyCommand command);
}
