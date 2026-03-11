package com.melissa.diary.scheduler;

import com.melissa.diary.domain.UserSetting;
import com.melissa.diary.repository.UserSettingRepository;
import com.melissa.diary.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Push notification scheduler.
 * Runs every 10 minutes in KST.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final int RETRY_INTERVAL_MINUTES = 10;
    private static final int MAX_RETRY_COUNT = 6;

    private final UserSettingRepository userSettingRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 */10 * * * *", zone = "Asia/Seoul")
    public void sendDailyNotifications() {
        long startTime = System.currentTimeMillis();
        ZonedDateTime nowInKst = ZonedDateTime.now(KST_ZONE_ID);
        LocalDate todayInKst = nowInKst.toLocalDate();
        LocalDateTime retryEligibleBefore = nowInKst.toLocalDateTime().minusMinutes(RETRY_INTERVAL_MINUTES);
        LocalTime targetTime = floorToTenMinuteSlot(nowInKst.toLocalTime());
        Time notificationTime = Time.valueOf(targetTime);

        log.info(
                "[NotificationScheduler] started. zone={}, now={}, targetTime={}, retryInterval={}m, maxRetryCount={}",
                KST_ZONE_ID,
                nowInKst.toLocalDateTime(),
                targetTime,
                RETRY_INTERVAL_MINUTES,
                MAX_RETRY_COUNT
        );

        try {
            List<UserSetting> targets = userSettingRepository.findNotificationTargets(
                    notificationTime,
                    todayInKst,
                    retryEligibleBefore,
                    MAX_RETRY_COUNT
            );

            if (targets.isEmpty()) {
                log.info("[NotificationScheduler] no targets. targetTime={}", targetTime);
                return;
            }

            log.info("[NotificationScheduler] targets fetched. count={}", targets.size());

            notificationService.sendBatchNotifications(targets);

            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("[NotificationScheduler] completed. targetCount={}, elapsed={}ms", targets.size(), elapsedTime);

        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.error("[NotificationScheduler] failed. elapsed={}ms", elapsedTime, e);
        }
    }

    static LocalTime floorToTenMinuteSlot(LocalTime time) {
        int minuteSlot = (time.getMinute() / 10) * 10;
        return LocalTime.of(time.getHour(), minuteSlot, 0);
    }
}
