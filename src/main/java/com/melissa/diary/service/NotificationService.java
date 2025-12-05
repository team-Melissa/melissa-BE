package com.melissa.diary.service;

import com.melissa.diary.domain.ExpoPushToken;
import com.melissa.diary.domain.UserSetting;
import com.melissa.diary.repository.ExpoPushTokenRepository;
import com.melissa.diary.repository.UserSettingRepository;
import com.melissa.diary.web.dto.ExpoPushNotificationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
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
     * 배치 단위로 알림 발송
     * - 각 사용자별로 독립적인 트랜잭션
     * - 개별 실패가 전체에 영향 없음
     */
    public void sendBatchNotifications(List<UserSetting> userSettings) {
        int successCount = 0;
        int failCount = 0;
        
        log.info("[Notification] 배치 알림 발송 시작. 대상: {}명", userSettings.size());
        
        for (UserSetting userSetting : userSettings) {
            try {
                sendNotificationToUser(userSetting);
                successCount++;
            } catch (Exception e) {
                log.error("[Notification] 사용자 알림 발송 실패. userId={}", 
                        userSetting.getUser().getId(), e);
                failCount++;
            }
        }
        
        log.info("[Notification] 배치 알림 발송 완료. 성공: {}, 실패: {}", successCount, failCount);
    }
    
    /**
     * 개별 사용자에게 알림 발송
     * - 해당 사용자의 모든 유효한 토큰에 발송
     * - 하나라도 성공하면 lastSentDate 갱신
     */
    @Transactional
    public void sendNotificationToUser(UserSetting userSetting) {
        Long userId = userSetting.getUser().getId();
        
        // 유효한 토큰 목록 조회
        List<ExpoPushToken> validTokens = userSetting.getUser().getExpoPushTokenList().stream()
                .filter(token -> !token.getInvalid())
                .toList();
        
        if (validTokens.isEmpty()) {
            log.warn("[Notification] 유효한 토큰 없음. userId={}", userId);
            return;
        }
        
        log.info("[Notification] 사용자 알림 발송 시작. userId={}, 토큰 수={}", userId, validTokens.size());
        
        boolean anySuccess = false;
        int tokenSuccessCount = 0;
        int tokenFailCount = 0;
        
        // 알림 메시지 구성
        String title = buildNotificationTitle(userSetting);
        String body = buildNotificationBody(userSetting);
        
        // 각 토큰에 발송
        for (ExpoPushToken token : validTokens) {
            try {
                sendPushNotification(token.getExpoPushToken(), title, body);
                anySuccess = true;
                tokenSuccessCount++;
                log.info("[Notification] 토큰 발송 성공. userId={}, tokenId={}", userId, token.getId());
            } catch (InvalidTokenException e) {
                // Invalid 토큰은 비활성화
                markTokenAsInvalid(token.getId());
                tokenFailCount++;
                log.warn("[Notification] Invalid 토큰 비활성화. userId={}, tokenId={}", userId, token.getId());
            } catch (Exception e) {
                tokenFailCount++;
                log.error("[Notification] 토큰 발송 실패. userId={}, tokenId={}", userId, token.getId(), e);
            }
        }
        
        // 하나라도 성공하면 lastSentDate 갱신
        if (anySuccess) {
            updateLastSentDate(userSetting.getId());
            log.info("[Notification] 사용자 알림 발송 완료. userId={}, 토큰 성공: {}, 실패: {}", 
                    userId, tokenSuccessCount, tokenFailCount);
        } else {
            log.error("[Notification] 모든 토큰 발송 실패. userId={}", userId);
        }
    }
    
    /**
     * Expo Push API 호출
     * @throws InvalidTokenException Invalid 토큰인 경우
     * @throws RuntimeException 기타 오류
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
     * 토큰을 Invalid로 표시
     */
    @Transactional
    public void markTokenAsInvalid(Long tokenId) {
        expoPushTokenRepository.findById(tokenId).ifPresent(token -> {
            token.setInvalid(true);
            expoPushTokenRepository.save(token);
            log.info("[Notification] 토큰 비활성화 완료. tokenId={}", tokenId);
        });
    }
    
    /**
     * lastSentDate 갱신
     */
    @Transactional
    public void updateLastSentDate(Long userSettingId) {
        userSettingRepository.findById(userSettingId).ifPresent(setting -> {
            setting.setLastSentDate(LocalDate.now());
            userSettingRepository.save(setting);
            log.debug("[Notification] lastSentDate 갱신 완료. userSettingId={}, date={}", 
                    userSettingId, LocalDate.now());
        });
    }
    
    /**
     * 알림 제목 생성
     */
    private String buildNotificationTitle(UserSetting userSetting) {
        if (userSetting.isNotificationSummary() && userSetting.isNotificationQna()) {
            return "📝 오늘의 일기를 작성해보세요!";
        } else if (userSetting.isNotificationSummary()) {
            return "📝 오늘 하루를 정리해보세요";
        } else {
            return "💬 AI와 대화를 시작해보세요";
        }
    }
    
    /**
     * 알림 본문 생성
     */
    private String buildNotificationBody(UserSetting userSetting) {
        if (userSetting.isNotificationSummary() && userSetting.isNotificationQna()) {
            return "오늘 하루는 어땠나요? AI와 대화하며 일기를 작성해보세요.";
        } else if (userSetting.isNotificationSummary()) {
            return "오늘의 기억을 일기로 남겨보세요.";
        } else {
            return "오늘 하루에 대해 이야기해보세요.";
        }
    }
    
    /**
     * Invalid 토큰 예외
     */
    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }
    }
}

