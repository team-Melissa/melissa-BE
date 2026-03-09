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
 * Expo Push API 기반 푸시 알림 배치 발송 서비스
 */
@Slf4j
@Service
public class NotificationService {

    private static final int BATCH_SIZE = 100;

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
     * 스케줄러 스레드 블로킹을 피하기 위한 비동기 진입점
     */
    @Async("notificationExecutor")
    public void sendBatchNotifications(List<UserSetting> userSettings) {
        int totalCount = userSettings.size();
        int batchCount = (int) Math.ceil((double) totalCount / BATCH_SIZE);
        long totalStartTime = System.currentTimeMillis();

        int totalSuccess = 0;
        int totalFail = 0;

        log.info("[Notification] batch notification started. totalUsers={}, batches={}, thread={}",
                totalCount, batchCount, Thread.currentThread().getName());

        for (int i = 0; i < totalCount; i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, totalCount);
            List<UserSetting> batch = userSettings.subList(i, endIndex);
            int batchNumber = (i / BATCH_SIZE) + 1;

            try {
                int[] result = processBatchWithTransaction(batch, batchNumber, batchCount);
                totalSuccess += result[0];
                totalFail += result[1];
            } catch (Exception e) {
                log.error("[Notification] batch {}/{} failed", batchNumber, batchCount, e);
                totalFail += batch.size();
            }
        }

        long totalElapsedTime = System.currentTimeMillis() - totalStartTime;
        log.info("[Notification] batch notification completed. totalUsers={}, success={}, fail={}, elapsed={}ms, thread={}",
                totalCount, totalSuccess, totalFail, totalElapsedTime, Thread.currentThread().getName());
    }

    /**
     * 배치 단위를 새로운 트랜잭션으로 처리
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int[] processBatchWithTransaction(List<UserSetting> batch, int batchNumber, int totalBatches) {
        long batchStartTime = System.currentTimeMillis();
        int successCount = 0;
        int failCount = 0;

        log.info("[Notification] batch {}/{} started. size={}", batchNumber, totalBatches, batch.size());

        for (UserSetting userSetting : batch) {
            try {
                sendNotificationToUser(userSetting);
                successCount++;
            } catch (Exception e) {
                log.error("[Notification] failed to send notification to userId={}",
                        userSetting.getUser().getId(), e);
                failCount++;
            }
        }

        long batchElapsedTime = System.currentTimeMillis() - batchStartTime;
        log.info("[Notification] batch {}/{} completed. success={}, fail={}, elapsed={}ms",
                batchNumber, totalBatches, successCount, failCount, batchElapsedTime);

        return new int[]{successCount, failCount};
    }

    /**
     * 단일 사용자 알림 발송
     */
    public void sendNotificationToUser(UserSetting userSetting) {
        Long userId = userSetting.getUser().getId();

        // 날짜 기준 중복 발송 방지: 먼저 lastSentDate를 갱신한 뒤 발송
        boolean updated = updateLastSentDateIfNotTodayInternal(userSetting);
        if (!updated) {
            log.debug("[Notification] already sent today. userId={}", userId);
            return;
        }

        List<ExpoPushToken> validTokens = userSetting.getUser().getExpoPushTokenList().stream()
                .filter(token -> !token.getInvalid())
                .toList();

        if (validTokens.isEmpty()) {
            log.warn("[Notification] no valid token. userId={}", userId);
            return;
        }

        log.info("[Notification] sending notification. userId={}, tokenCount={}", userId, validTokens.size());

        int tokenSuccessCount = 0;
        int tokenFailCount = 0;

        String title = buildNotificationTitle(userSetting);
        String body = buildNotificationBody(userSetting);

        for (ExpoPushToken token : validTokens) {
            try {
                sendPushNotification(token.getExpoPushToken(), title, body);
                tokenSuccessCount++;
                log.info("[Notification] token send success. userId={}, tokenId={}", userId, token.getId());
            } catch (InvalidTokenException e) {
                markTokenAsInvalidInternal(token);
                tokenFailCount++;
                log.warn("[Notification] invalid token disabled. userId={}, tokenId={}", userId, token.getId());
            } catch (Exception e) {
                tokenFailCount++;
                log.error("[Notification] token send failed. userId={}, tokenId={}", userId, token.getId(), e);
            }
        }

        log.info("[Notification] user notification completed. userId={}, success={}, fail={}",
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
                        log.error("[Notification] Expo API call failed. token={}", token, e);
                        return Mono.error(new RuntimeException("Expo API call failed", e));
                    })
                    .block();

            if (response != null && response.getData() != null && !response.getData().isEmpty()) {
                ExpoPushNotificationDTO.PushTicket ticket = response.getData().get(0);

                if ("error".equals(ticket.getStatus())) {
                    String errorType = ticket.getDetails() != null ? ticket.getDetails().getError() : "Unknown";

                    if ("DeviceNotRegistered".equals(errorType) || "InvalidCredentials".equals(errorType)) {
                        throw new InvalidTokenException(ticket.getMessage());
                    }

                    throw new RuntimeException("Expo push failed: " + ticket.getMessage());
                }
            }

        } catch (InvalidTokenException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Notification] notification send exception. token={}", token, e);
            throw new RuntimeException("Notification send failed", e);
        }
    }

    /**
     * 같은 배치 트랜잭션 내에서 Invalid 토큰 처리
     */
    private void markTokenAsInvalidInternal(ExpoPushToken token) {
        try {
            token.markInvalid();
            expoPushTokenRepository.save(token);
            log.info("[Notification] token disabled. tokenId={}", token.getId());
        } catch (Exception e) {
            log.error("[Notification] failed to disable token. tokenId={}", token.getId(), e);
        }
    }

    /**
     * 오늘 아직 발송하지 않은 경우에만 lastSentDate 갱신
     */
    private boolean updateLastSentDateIfNotTodayInternal(UserSetting setting) {
        try {
            LocalDate today = LocalDate.now();

            if (today.equals(setting.getLastSentDate())) {
                return false;
            }

            setting.setLastSentDate(today);
            userSettingRepository.save(setting);
            log.debug("[Notification] lastSentDate updated. userSettingId={}, date={}", setting.getId(), today);
            return true;

        } catch (Exception e) {
            log.error("[Notification] failed to update lastSentDate. userSettingId={}", setting.getId(), e);
            return false;
        }
    }

    private String buildNotificationTitle(UserSetting userSetting) {
        return "📝 오늘의 일기를 작성해보세요!";
    }

    private String buildNotificationBody(UserSetting userSetting) {
        return "오늘 하루는 어떠셨나요? 멜리사와 대화하며 일기를 작성해보세요.";
    }

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }
    }
}
