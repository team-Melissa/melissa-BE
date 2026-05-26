package com.melissa.diary.domain.enums;

public enum OutboxEventStatus {
    NEW,
    PUBLISHED,
    FAILED_RETRYABLE
}
