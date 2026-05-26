package com.melissa.diary.domain.enums;

public enum AsyncJobStatus {
    PENDING,
    PROCESSING,
    SUCCEEDED,
    FAILED_RETRYABLE,
    FAILED_FINAL,
    CANCELLED
}
