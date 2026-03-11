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
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * 푸시 알림 스케줄러
 * 매 10분마다 실행하여 해당 시간 알림 설정 사용자에게 발송
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final UserSettingRepository userSettingRepository;
    private final NotificationService notificationService;

    /**
     * 10분 단위 알림 발송 (00, 10, 20, 30, 40, 50분)
     */
    @Scheduled(cron = "0 */10 * * * *", zone = "Asia/Seoul")
    public void sendDailyNotifications() {
        long startTime = System.currentTimeMillis();
        ZonedDateTime nowInKst = ZonedDateTime.now(KST_ZONE_ID);
        LocalDate todayInKst = nowInKst.toLocalDate();
        LocalTime targetTime = floorToTenMinuteSlot(nowInKst.toLocalTime());
        Time notificationTime = Time.valueOf(targetTime);

        log.info("[NotificationScheduler] 알림 스케줄러 시작. zone={}, now={}, targetTime={}",
                KST_ZONE_ID, nowInKst.toLocalDateTime(), targetTime);

        try {
            List<UserSetting> targets = userSettingRepository.findNotificationTargets(
                    notificationTime,
                    todayInKst
            );

            if (targets.isEmpty()) {
                log.info("[NotificationScheduler] 발송 대상 없음. 시간: {}", targetTime);
                return;
            }

            log.info("[NotificationScheduler] 발송 대상 조회 완료. 대상: {}명", targets.size());

            notificationService.sendBatchNotifications(targets);

            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("[NotificationScheduler] 알림 스케줄러 완료 (배치 순차 발송 시작). 대상: {}명, 조회 시간: {}ms",
                    targets.size(), elapsedTime);

        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.error("[NotificationScheduler] 알림 스케줄러 실행 중 오류 발생. 소요 시간: {}ms", elapsedTime, e);
        }
    }

    static LocalTime floorToTenMinuteSlot(LocalTime time) {
        int minuteSlot = (time.getMinute() / 10) * 10;
        return LocalTime.of(time.getHour(), minuteSlot, 0);
    }
}
