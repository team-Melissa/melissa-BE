package com.melissa.diary.service.payment.store;

public interface GooglePlayPurchaseVerifier {

    VerifiedPurchase verify(GooglePurchaseVerifyCommand command);

    void acknowledge(GoogleAcknowledgeCommand command);
}
