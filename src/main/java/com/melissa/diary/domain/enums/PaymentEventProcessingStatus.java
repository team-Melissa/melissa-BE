package com.melissa.diary.domain.enums;

public enum PaymentEventProcessingStatus {
    RECEIVED,
    PROCESSED,
    FAILED_RETRYABLE,
    FAILED_FINAL,
    DUPLICATE
}
