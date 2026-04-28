package com.melissa.diary.retry;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public final class RetryExecutor {

    private RetryExecutor() {
    }

    public static <T> T execute(String operationName, RetryPolicy policy, RetryableOperation<T> operation) {
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= policy.maxAttempts(); attempt++) {
            try {
                return operation.run();
            } catch (Exception e) {
                RuntimeException failure = toRuntimeException(e);
                lastFailure = failure;

                boolean retryable = RetryClassifier.isRetryable(failure);
                boolean hasRemainingAttempt = attempt < policy.maxAttempts();

                if (!retryable || !hasRemainingAttempt) {
                    log.warn("[Retry] operation failed final. operation={}, attempt={}, maxAttempts={}, retryable={}",
                            operationName, attempt, policy.maxAttempts(), retryable, failure);
                    throw failure;
                }

                Duration delay = applyJitter(policy.backoffForAttempt(attempt), policy.jitterRatio());
                log.warn("[Retry] operation failed. operation={}, attempt={}, maxAttempts={}, nextDelayMs={}",
                        operationName, attempt, policy.maxAttempts(), delay.toMillis(), failure);
                sleep(delay);
            }
        }

        throw lastFailure != null ? lastFailure : new IllegalStateException("retry operation failed");
    }

    private static RuntimeException toRuntimeException(Exception exception) {
        if (exception instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new RuntimeException(exception);
    }

    private static Duration applyJitter(Duration baseDelay, double jitterRatio) {
        if (baseDelay.isZero() || baseDelay.isNegative() || jitterRatio <= 0) {
            return baseDelay;
        }

        long baseMillis = baseDelay.toMillis();
        long jitterMillis = Math.round(baseMillis * jitterRatio);
        long offset = ThreadLocalRandom.current().nextLong(-jitterMillis, jitterMillis + 1);
        return Duration.ofMillis(Math.max(0, baseMillis + offset));
    }

    private static void sleep(Duration delay) {
        if (delay.isZero() || delay.isNegative()) {
            return;
        }

        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("retry interrupted", e);
        }
    }

    @FunctionalInterface
    public interface RetryableOperation<T> {
        T run() throws Exception;
    }
}
