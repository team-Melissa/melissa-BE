package com.melissa.diary.domain.enums;

public enum IdempotencyStatus {
    PENDING,
    SUCCEEDED,
    FAILED_RETRYABLE,
    FAILED_FINAL
}
