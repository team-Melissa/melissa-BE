package com.melissa.diary.retry;

import java.time.Duration;
import java.util.List;

public record RetryPolicy(
        int maxAttempts,
        List<Duration> backoffs,
        double jitterRatio
) {
    public static final RetryPolicy S3_UPLOAD = new RetryPolicy(
            3,
            List.of(Duration.ofMillis(500), Duration.ofSeconds(2)),
            0.2
    );

    public static final RetryPolicy DIARY_IMAGE = new RetryPolicy(
            5,
            List.of(
                    Duration.ofMinutes(1),
                    Duration.ofMinutes(5),
                    Duration.ofMinutes(15),
                    Duration.ofHours(1)
            ),
            0.2
    );

    public static final RetryPolicy MEMORY_BATCH = new RetryPolicy(
            3,
            List.of(Duration.ofMinutes(15), Duration.ofHours(1)),
            0.2
    );

    public static final RetryPolicy EXPO_PUSH = new RetryPolicy(
            6,
            List.of(Duration.ofMinutes(10)),
            0.0
    );

    public Duration backoffForAttempt(int attemptNumber) {
        if (attemptNumber <= 0 || backoffs.isEmpty()) {
            return Duration.ZERO;
        }
        int index = Math.min(attemptNumber - 1, backoffs.size() - 1);
        return backoffs.get(index);
    }
}
