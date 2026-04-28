package com.melissa.diary.service;

import com.melissa.diary.domain.ExpoPushToken;
import com.melissa.diary.domain.UserSetting;
import com.melissa.diary.repository.ExpoPushTokenRepository;
import com.melissa.diary.repository.UserSettingRepository;
import com.melissa.diary.retry.RetryClassifier;
import com.melissa.diary.retry.RetryPolicy;
import com.melissa.diary.web.dto.ExpoPushNotificationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Expo Push API 기반 푸시 알림 배치 발송 서비스
 */
@Slf4j
@Service
public class NotificationService {

    private static final int BATCH_SIZE = 100;
    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final int MAX_RETRY_COUNT = RetryPolicy.EXPO_PUSH.maxAttempts();

    private final UserSettingRepository userSettingRepository;
    private final ExpoPushTokenRepository expoPushTokenRepository;
    private final WebClient expoWebClient;
    private final TransactionTemplate transactionTemplate;

    public NotificationService(UserSettingRepository userSettingRepository,
                               ExpoPushTokenRepository expoPushTokenRepository,
                               @Qualifier("expoWebClient") WebClient expoWebClient,
                               TransactionTemplate transactionTemplate) {
        this.userSettingRepository = userSettingRepository;
        this.expoPushTokenRepository = expoPushTokenRepository;
        this.expoWebClient = expoWebClient;
        this.transactionTemplate = transactionTemplate;
    }

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

    public int[] processBatchWithTransaction(List<UserSetting> batch, int batchNumber, int totalBatches) {
        long batchStartTime = System.currentTimeMillis();
        int successCount = 0;
        int failCount = 0;

        log.info("[Notification] batch {}/{} started. size={}", batchNumber, totalBatches, batch.size());

        for (UserSetting userSetting : batch) {
            try {
                boolean delivered = sendNotificationToUser(userSetting);
                if (delivered) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                log.error("[Notification] failed to send notification to userId={}", userSetting.getUser().getId(), e);
                failCount++;
            }
        }

        long batchElapsedTime = System.currentTimeMillis() - batchStartTime;
        log.info("[Notification] batch {}/{} completed. success={}, fail={}, elapsed={}ms",
                batchNumber, totalBatches, successCount, failCount, batchElapsedTime);

        return new int[]{successCount, failCount};
    }

    /**
     * @return true when at least one push was delivered successfully.
     */
    public boolean sendNotificationToUser(UserSetting userSetting) {
        Long userId = userSetting.getUser().getId();
        LocalDate today = LocalDate.now(KST_ZONE_ID);
        LocalDateTime now = LocalDateTime.now(KST_ZONE_ID);

        if (isAlreadySentToday(userSetting, today)) {
            log.debug("[Notification] already sent today. userId={}", userId);
            return true;
        }

        List<ExpoPushToken> validTokens = userSetting.getUser().getExpoPushTokenList().stream()
                .filter(token -> !token.getInvalid())
                .toList();

        if (validTokens.isEmpty()) {
            log.warn("[Notification] no valid token. userId={}", userId);
            markNotificationAttemptFailedInternal(userSetting, today, now, "NO_VALID_TOKEN");
            return false;
        }

        log.info("[Notification] sending notification. userId={}, tokenCount={}", userId, validTokens.size());

        int tokenSuccessCount = 0;
        int tokenFailCount = 0;
        int retryableFailCount = 0;

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
            } catch (NonRetryablePushException e) {
                tokenFailCount++;
                log.warn("[Notification] non-retryable token send failed. userId={}, tokenId={}, reason={}",
                        userId, token.getId(), e.getMessage());
            } catch (Exception e) {
                tokenFailCount++;
                retryableFailCount++;
                log.error("[Notification] token send failed. userId={}, tokenId={}", userId, token.getId(), e);
            }
        }

        if (tokenSuccessCount > 0) {
            markNotificationSuccessInternal(userSetting, today, now);
            log.info("[Notification] user notification completed. userId={}, success={}, fail={}",
                    userId, tokenSuccessCount, tokenFailCount);
            return true;
        }

        if (retryableFailCount > 0) {
            markNotificationAttemptFailedInternal(userSetting, today, now, "ALL_TOKEN_SEND_FAILED");
        } else {
            markNotificationFinalFailedInternal(userSetting, now, "NON_RETRYABLE_TOKEN_FAILURES");
        }
        log.warn("[Notification] user notification failed. userId={}, success={}, fail={}",
                userId, tokenSuccessCount, tokenFailCount);
        return false;
    }

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
                    .block();

            if (response != null && response.getData() != null && !response.getData().isEmpty()) {
                ExpoPushNotificationDTO.PushTicket ticket = response.getData().get(0);

                if ("error".equals(ticket.getStatus())) {
                    String errorType = ticket.getDetails() != null ? ticket.getDetails().getError() : "Unknown";

                    if ("DeviceNotRegistered".equals(errorType) || "InvalidCredentials".equals(errorType)) {
                        throw new InvalidTokenException(ticket.getMessage());
                    }

                    if ("MessageRateExceeded".equals(errorType) || "ProviderError".equals(errorType)) {
                        throw new RetryablePushException("Expo push retryable ticket error: " + ticket.getMessage());
                    }

                    throw new NonRetryablePushException("Expo push non-retryable ticket error: " + ticket.getMessage());
                }
            }

        } catch (InvalidTokenException e) {
            throw e;
        } catch (RetryablePushException | NonRetryablePushException e) {
            throw e;
        } catch (WebClientResponseException e) {
            log.error("[Notification] Expo API response error. token={}, status={}, body={}",
                    token, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw classifyPushException("Expo API response error", e);
        } catch (Exception e) {
            log.error("[Notification] notification send exception. token={}", token, e);
            throw classifyPushException("Notification send failed", e);
        }
    }

    private boolean isAlreadySentToday(UserSetting setting, LocalDate today) {
        return today.equals(setting.getLastSentDate());
    }

    private void markNotificationSuccessInternal(UserSetting setting, LocalDate today, LocalDateTime now) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                setting.setLastSentDate(today);
                setting.setLastAttemptAt(now);
                setting.setRetryCount(0);
                userSettingRepository.save(setting);
            });
            log.info("[Notification] delivery state marked success. userSettingId={}, date={}", setting.getId(), today);
        } catch (Exception e) {
            log.error("[Notification] failed to mark success state. userSettingId={}", setting.getId(), e);
        }
    }

    private void markNotificationAttemptFailedInternal(
            UserSetting setting,
            LocalDate today,
            LocalDateTime now,
            String reason
    ) {
        try {
            int nextRetryCount = calculateNextRetryCount(setting, today);
            transactionTemplate.executeWithoutResult(status -> {
                setting.setRetryCount(nextRetryCount);
                setting.setLastAttemptAt(now);
                userSettingRepository.save(setting);
            });
            log.info("[Notification] delivery state marked failure. userSettingId={}, retryCount={}, reason={}",
                    setting.getId(), nextRetryCount, reason);
        } catch (Exception e) {
            log.error("[Notification] failed to mark failure state. userSettingId={}", setting.getId(), e);
        }
    }

    private void markNotificationFinalFailedInternal(
            UserSetting setting,
            LocalDateTime now,
            String reason
    ) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                setting.setRetryCount(MAX_RETRY_COUNT);
                setting.setLastAttemptAt(now);
                userSettingRepository.save(setting);
            });
            log.info("[Notification] delivery state marked final failure. userSettingId={}, reason={}",
                    setting.getId(), reason);
        } catch (Exception e) {
            log.error("[Notification] failed to mark final failure state. userSettingId={}", setting.getId(), e);
        }
    }

    private int calculateNextRetryCount(UserSetting setting, LocalDate today) {
        Integer currentRetryCount = setting.getRetryCount() == null ? 0 : setting.getRetryCount();
        LocalDateTime lastAttemptAt = setting.getLastAttemptAt();

        if (lastAttemptAt == null || !today.equals(lastAttemptAt.toLocalDate())) {
            return 1;
        }

        return currentRetryCount + 1;
    }

    private void markTokenAsInvalidInternal(ExpoPushToken token) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                token.markInvalid();
                expoPushTokenRepository.save(token);
            });
            log.info("[Notification] token disabled. tokenId={}", token.getId());
        } catch (Exception e) {
            log.error("[Notification] failed to disable token. tokenId={}", token.getId(), e);
        }
    }

    private String buildNotificationTitle(UserSetting userSetting) {
        return "📖 오늘의 일기를 작성해보세요!";
    }

    private String buildNotificationBody(UserSetting userSetting) {
        return "오늘 하루는 어떠셨나요? 멜리사와 대화하며 일기를 작성해보세요.";
    }

    private RuntimeException classifyPushException(String message, Throwable throwable) {
        if (RetryClassifier.isRetryable(throwable)) {
            return new RetryablePushException(message, throwable);
        }
        return new NonRetryablePushException(message, throwable);
    }

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) {
            super(message);
        }
    }

    public static class RetryablePushException extends RuntimeException {
        public RetryablePushException(String message) {
            super(message);
        }

        public RetryablePushException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class NonRetryablePushException extends RuntimeException {
        public NonRetryablePushException(String message) {
            super(message);
        }

        public NonRetryablePushException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
