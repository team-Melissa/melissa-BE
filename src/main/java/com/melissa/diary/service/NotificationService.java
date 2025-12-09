package com.melissa.diary.service;

import com.melissa.diary.domain.ExpoPushToken;
import com.melissa.diary.domain.UserSetting;
import com.melissa.diary.repository.ExpoPushTokenRepository;
import com.melissa.diary.repository.UserSettingRepository;
import com.melissa.diary.web.dto.ExpoPushNotificationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

/**
 * 푸시 알림 발송 서비스
 */
@Slf4j
@Service
public class NotificationService {
    
    private static final int BATCH_SIZE = 100;  // 배치당 처리 인원
    
    private final UserSettingRepository userSettingRepository;
    private final ExpoPushTokenRepository expoPushTokenRepository;
    private final WebClient expoWebClient;
    
    public NotificationService(UserSettingRepository userSettingRepository,
                              ExpoPushTokenRepository expoPushTokenRepository,
                              @Qualifier("expoWebClient") WebClient expoWebClient) {
        this.userSettingRepository = userSettingRepository;
        this.expoPushTokenRepository = expoPushTokenRepository;
        this.expoWebClient = expoWebClient;
    }
    
    /**
     * 배치 순차 발송
     * 비동기 시작으로 스케줄러 블로킹 방지, 100명씩 배치 트랜잭션으로 I/O 최적화
     */
    @Async("notificationExecutor")
    public void sendBatchNotifications(List<UserSetting> userSettings) {
        int totalCount = userSettings.size();
        int batchCount = (int) Math.ceil((double) totalCount / BATCH_SIZE);
        long totalStartTime = System.currentTimeMillis();
        
        int totalSuccess = 0;
        int totalFail = 0;
        
        log.info("[Notification] 배치 순차 발송 시작. 전체: {}명, 배치 수: {}, 스레드: {}", 
                totalCount, batchCount, Thread.currentThread().getName());
        
        for (int i = 0; i < totalCount; i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, totalCount);
            List<UserSetting> batch = userSettings.subList(i, endIndex);
            int batchNumber = (i / BATCH_SIZE) + 1;
            
            // 배치 단위 트랜잭션 처리
            try {
                int[] result = processBatchWithTransaction(batch, batchNumber, batchCount);
                totalSuccess += result[0];
                totalFail += result[1];
            } catch (Exception e) {
                log.error("[Notification] 배치 {}/{} 처리 실패", batchNumber, batchCount, e);
                totalFail += batch.size();
            }
        }
        
        long totalElapsedTime = System.currentTimeMillis() - totalStartTime;
        log.info("[Notification] 전체 발송 완료. 전체: {}명, 성공: {}, 실패: {}, 총 소요: {}ms, 스레드: {}", 
                totalCount, totalSuccess, totalFail, totalElapsedTime, Thread.currentThread().getName());
    }
    
    /**
     * 배치 단위 트랜잭션 처리 (100명씩)
     * DB I/O 최적화를 위해 배치를 하나의 트랜잭션으로 처리
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int[] processBatchWithTransaction(List<UserSetting> batch, int batchNumber, int totalBatches) {
        long batchStartTime = System.currentTimeMillis();
        int successCount = 0;
        int failCount = 0;
        
        log.info("[Notification] 배치 {}/{} 발송 시작. 대상: {}명", 
                batchNumber, totalBatches, batch.size());
        
        for (UserSetting userSetting : batch) {
            try {
                sendNotificationToUser(userSetting);
                successCount++;
            } catch (Exception e) {
                log.error("[Notification] 사용자 알림 발송 실패. userId={}", 
                        userSetting.getUser().getId(), e);
                failCount++;
            }
        }
        
        long batchElapsedTime = System.currentTimeMillis() - batchStartTime;
        log.info("[Notification] 배치 {}/{} 발송 완료. 성공: {}, 실패: {}, 소요: {}ms", 
                batchNumber, totalBatches, successCount, failCount, batchElapsedTime);
        
        return new int[]{successCount, failCount};
    }
    
    /**
     * 개별 사용자 알림 발송
     * 중복 발송 방지를 위해 발송 전 lastSentDate 먼저 갱신
     * 배치 트랜잭션 내에서 실행됨
     */
    public void sendNotificationToUser(UserSetting userSetting) {
        Long userId = userSetting.getUser().getId();
        
        // 중복 발송 방지: 발송 전에 먼저 lastSentDate 갱신 (같은 트랜잭션 내)
        boolean updated = updateLastSentDateIfNotTodayInternal(userSetting);
        if (!updated) {
            log.debug("[Notification] 이미 오늘 발송됨. userId={}", userId);
            return;
        }
        
        // 유효한 토큰 목록 조회
        List<ExpoPushToken> validTokens = userSetting.getUser().getExpoPushTokenList().stream()
                .filter(token -> !token.getInvalid())
                .toList();
        
        if (validTokens.isEmpty()) {
            log.warn("[Notification] 유효한 토큰 없음. userId={}", userId);
            return;
        }
        
        log.info("[Notification] 사용자 알림 발송 시작. userId={}, 토큰 수={}", userId, validTokens.size());
        
        int tokenSuccessCount = 0;
        int tokenFailCount = 0;
        
        // 알림 메시지 구성
        String title = buildNotificationTitle(userSetting);
        String body = buildNotificationBody(userSetting);
        
        // 각 토큰에 발송
        for (ExpoPushToken token : validTokens) {
            try {
                sendPushNotification(token.getExpoPushToken(), title, body);
                tokenSuccessCount++;
                log.info("[Notification] 토큰 발송 성공. userId={}, tokenId={}", userId, token.getId());
            } catch (InvalidTokenException e) {
                // Invalid 토큰은 비활성화 (같은 트랜잭션 내)
                markTokenAsInvalidInternal(token);
                tokenFailCount++;
                log.warn("[Notification] Invalid 토큰 비활성화. userId={}, tokenId={}", userId, token.getId());
            } catch (Exception e) {
                tokenFailCount++;
                log.error("[Notification] 토큰 발송 실패. userId={}, tokenId={}", userId, token.getId(), e);
            }
        }
        
        log.info("[Notification] 사용자 알림 발송 완료. userId={}, 토큰 성공: {}, 실패: {}", 
                userId, tokenSuccessCount, tokenFailCount);
    }
    
    /**
     * Expo Push API 호출
     */
    private void sendPushNotification(String token, String title, String body) {
        ExpoPushNotificationDTO.PushRequest request = ExpoPushNotificationDTO.PushRequest.builder()
                .to(token)
                .title(title)
                .body(body)
                .sound("default")
                .priority("high")
                .build();
        
        try {
            ExpoPushNotificationDTO.PushResponse response = expoWebClient.post()
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ExpoPushNotificationDTO.PushResponse.class)
                    .onErrorResume(e -> {
                        log.error("[Notification] Expo API 호출 실패. token={}", token, e);
                        return Mono.error(new RuntimeException("Expo API 호출 실패", e));
                    })
                    .block();
            
            // 응답 검증
            if (response != null && response.getData() != null && !response.getData().isEmpty()) {
                ExpoPushNotificationDTO.PushTicket ticket = response.getData().get(0);
                
                if ("error".equals(ticket.getStatus())) {
                    String errorType = ticket.getDetails() != null ? ticket.getDetails().getError() : "Unknown";
                    
                    // DeviceNotRegistered는 Invalid 토큰으로 처리
                    if ("DeviceNotRegistered".equals(errorType) || "InvalidCredentials".equals(errorType)) {
                        throw new InvalidTokenException(ticket.getMessage());
                    }
                    
                    throw new RuntimeException("Expo Push 실패: " + ticket.getMessage());
                }
            }
            
        } catch (InvalidTokenException e) {
            throw e;  // 상위로 전파
        } catch (Exception e) {
            log.error("[Notification] 알림 발송 중 예외 발생. token={}", token, e);
            throw new RuntimeException("알림 발송 실패", e);
        }
    }
    
    /**
     * Invalid 토큰 비활성화 (배치 트랜잭션 내에서 실행)
     */
    private void markTokenAsInvalidInternal(ExpoPushToken token) {
        try {
            token.setInvalid(true);
            expoPushTokenRepository.save(token);
            log.info("[Notification] 토큰 비활성화 완료. tokenId={}", token.getId());
        } catch (Exception e) {
            log.error("[Notification] 토큰 비활성화 실패. tokenId={}", token.getId(), e);
        }
    }
    
    /**
     * 발송 전 lastSentDate 갱신 (배치 트랜잭션 내에서 실행)
     * @return 갱신 성공 여부 (true: 갱신됨, false: 이미 오늘 날짜)
     */
    private boolean updateLastSentDateIfNotTodayInternal(UserSetting setting) {
        try {
            LocalDate today = LocalDate.now();
            
            // 이미 오늘 발송했으면 false 반환
            if (today.equals(setting.getLastSentDate())) {
                return false;
            }
            
            // 오늘 날짜로 갱신
            setting.setLastSentDate(today);
            userSettingRepository.save(setting);
            log.debug("[Notification] lastSentDate 갱신 완료. userSettingId={}, date={}", 
                    setting.getId(), today);
            return true;
            
        } catch (Exception e) {
            log.error("[Notification] lastSentDate 갱신 실패. userSettingId={}", setting.getId(), e);
            return false;
        }
    }
    
    // 알림 제목 생성
    private String buildNotificationTitle(UserSetting userSetting) {
        return "📝 오늘의 일기를 작성해보세요!";
    }
    
    // 알림 본문 생성
    private String buildNotificationBody(UserSetting userSetting) {
        return "오늘 하루는 어땠나요? 멜리사와 대화하며 일기를 작성해보세요.";
    }
    
    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }
    }
}

