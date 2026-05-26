package com.melissa.diary.scheduler;

import com.melissa.diary.service.OutboxPublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "melissa.sqs", name = "enabled", havingValue = "true")
public class OutboxPublisherScheduler {

    private final OutboxPublisherService outboxPublisherService;

    @Scheduled(fixedDelayString = "${melissa.sqs.outbox-publisher-fixed-delay-ms:5000}")
    public void publishDueEvents() {
        try {
            outboxPublisherService.publishDueEvents();
        } catch (Exception e) {
            log.error("[OutboxPublisherScheduler] publish cycle failed", e);
        }
    }
}
