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
import java.util.List;

/**
 * 푸시 알림 스케줄러
 * - 매 10분마다 실행 (00, 10, 20, 30, 40, 50분)
 * - 해당 시간에 알림 받을 사용자에게 푸시 발송
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {
    
    private final UserSettingRepository userSettingRepository;
    private final NotificationService notificationService;
    
    /**
     * 10분 단위 알림 발송
     * - 00, 10, 20, 30, 40, 50분에 실행
     * - 해당 시간에 notificationTime이 설정된 사용자에게 알림 발송
     */
    @Scheduled(cron = "0 */10 * * * *", zone = "Asia/Seoul")
    public void sendDailyNotifications() {
        long startTime = System.currentTimeMillis();
        LocalTime now = LocalTime.now();
        
        // 현재 시각을 10분 단위로 내림 (예: 23:03 → 23:00, 23:17 → 23:10)
        int currentMinute = (now.getMinute() / 10) * 10;
        LocalTime targetTime = LocalTime.of(now.getHour(), currentMinute, 0);
        Time notificationTime = Time.valueOf(targetTime);
        
        log.info("[NotificationScheduler] 알림 스케줄러 시작. 대상 시간: {}", targetTime);
        
        try {
            // 발송 대상 조회
            List<UserSetting> targets = userSettingRepository.findNotificationTargets(
                    notificationTime, 
                    LocalDate.now()
            );
            
            if (targets.isEmpty()) {
                log.info("[NotificationScheduler] 발송 대상 없음. 시간: {}", targetTime);
                return;
            }
            
            log.info("[NotificationScheduler] 발송 대상 조회 완료. 대상: {}명", targets.size());
            
            // 배치 발송
            notificationService.sendBatchNotifications(targets);
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("[NotificationScheduler] 알림 스케줄러 완료. 대상: {}명, 소요 시간: {}ms", 
                    targets.size(), elapsedTime);
            
        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            log.error("[NotificationScheduler] 알림 스케줄러 실행 중 오류 발생. 소요 시간: {}ms", elapsedTime, e);
        }
    }
}

