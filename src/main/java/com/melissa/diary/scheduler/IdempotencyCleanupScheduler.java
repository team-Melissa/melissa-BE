package com.melissa.diary.scheduler;

import com.melissa.diary.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyCleanupScheduler {

    private final IdempotencyService idempotencyService;

    @Scheduled(cron = "${idempotency.cleanup-cron:0 0 * * * *}", zone = "Asia/Seoul")
    public void cleanupExpiredRecords() {
        long deleted = idempotencyService.cleanupExpiredRecords();
        if (deleted > 0) {
            log.info("[IdempotencyCleanup] removed expired records. count={}", deleted);
        }
    }
}
