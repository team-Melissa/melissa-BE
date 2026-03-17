package com.melissa.diary.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.domain.IdempotencyRecord;
import com.melissa.diary.domain.enums.IdempotencyStatus;
import com.melissa.diary.repository.IdempotencyRecordRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final long ttlHours;
    private final ObjectMapper objectMapper;

    public IdempotencyService(
            IdempotencyRecordRepository idempotencyRecordRepository,
            ObjectMapper objectMapper,
            @Value("${idempotency.ttl-hours:24}") long ttlHours
    ) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.objectMapper = objectMapper.copy()
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        this.ttlHours = ttlHours > 0 ? ttlHours : 24;
    }

    @Transactional
    public ClaimResult claim(Long userId, String endpoint, String idempotencyKey, Object request) {
        String normalizedKey = normalizeKey(idempotencyKey);
        String requestHash = hashRequest(request);
        LocalDateTime expiresAt = nextExpiration();

        int inserted = idempotencyRecordRepository.insertPendingIfAbsent(
                userId,
                endpoint,
                normalizedKey,
                requestHash,
                expiresAt
        );

        IdempotencyRecord existing = idempotencyRecordRepository.findForUpdate(userId, endpoint, normalizedKey)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus._INTERNAL_SERVER_ERROR));

        if (inserted > 0) {
            return new ClaimResult(ClaimAction.PROCEED, existing);
        }

        if (isExpired(existing)) {
            existing.setRequestHash(requestHash);
            existing.setExpiresAt(expiresAt);
            existing.markPending();
            return new ClaimResult(ClaimAction.PROCEED, existing);
        }

        validateRequestHash(existing, requestHash);

        if (existing.getStatus() == IdempotencyStatus.SUCCEEDED) {
            return new ClaimResult(ClaimAction.RETURN_CACHED, existing);
        }

        if (existing.getStatus() == IdempotencyStatus.FAILED_RETRYABLE) {
            existing.setExpiresAt(expiresAt);
            existing.markPending();
            return new ClaimResult(ClaimAction.PROCEED, existing);
        }

        throw new ErrorHandler(ErrorStatus.IDEMPOTENCY_REQUEST_IN_PROGRESS);
    }

    @Transactional
    public void markSucceeded(Long recordId, String responseBody) {
        IdempotencyRecord record = idempotencyRecordRepository.findById(recordId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus._INTERNAL_SERVER_ERROR));
        record.markSucceeded(responseBody);
    }

    @Transactional
    public void markFailedRetryable(Long recordId, Throwable throwable) {
        IdempotencyRecord record = idempotencyRecordRepository.findById(recordId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus._INTERNAL_SERVER_ERROR));
        record.markFailedRetryable(buildErrorMessage(throwable));
    }

    @Transactional
    public long cleanupExpiredRecords() {
        return idempotencyRecordRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    public String serialize(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new ErrorHandler(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
    }

    public <T> T deserialize(String body, Class<T> type) {
        try {
            return objectMapper.readValue(body, type);
        } catch (JsonProcessingException e) {
            throw new ErrorHandler(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
    }

    public String hashRequestForTest(Object request) {
        return hashRequest(request);
    }

    private String normalizeKey(String idempotencyKey) {
        String normalizedKey = idempotencyKey == null ? "" : idempotencyKey.trim();
        if (normalizedKey.isEmpty()) {
            throw new ErrorHandler(ErrorStatus._BAD_REQUEST);
        }
        return normalizedKey;
    }

    private String hashRequest(Object request) {
        try {
            byte[] payload = objectMapper.writeValueAsBytes(request);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(payload);
            return HexFormat.of().formatHex(digest);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new ErrorHandler(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
    }

    private void validateRequestHash(IdempotencyRecord existing, String requestHash) {
        if (!existing.getRequestHash().equals(requestHash)) {
            throw new ErrorHandler(ErrorStatus.IDEMPOTENCY_REQUEST_MISMATCH);
        }
    }

    private boolean isExpired(IdempotencyRecord record) {
        return record.getExpiresAt() != null && !record.getExpiresAt().isAfter(LocalDateTime.now());
    }

    private LocalDateTime nextExpiration() {
        return LocalDateTime.now().plusHours(ttlHours);
    }

    private String buildErrorMessage(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) {
            return "request failed";
        }
        String message = throwable.getMessage();
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    @Getter
    public static class ClaimResult {
        private final ClaimAction action;
        private final IdempotencyRecord record;

        public ClaimResult(ClaimAction action, IdempotencyRecord record) {
            this.action = action;
            this.record = record;
        }
    }

    public enum ClaimAction {
        PROCEED,
        RETURN_CACHED
    }
}
