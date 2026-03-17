package com.melissa.diary.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.domain.IdempotencyRecord;
import com.melissa.diary.domain.enums.IdempotencyStatus;
import com.melissa.diary.repository.IdempotencyRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        idempotencyService = new IdempotencyService(idempotencyRecordRepository, objectMapper, 24);
    }

    @Test
    void claimCreatesPendingRecordForNewKey() {
        IdempotencyRecord saved = IdempotencyRecord.builder()
                .id(1L)
                .userId(10L)
                .endpoint("POST:/api/v1/chats/message")
                .idempotencyKey("abc")
                .requestHash("hash")
                .status(IdempotencyStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        when(idempotencyRecordRepository.insertPendingIfAbsent(any(), any(), any(), any(), any())).thenReturn(1);
        when(idempotencyRecordRepository.findForUpdate(10L, "POST:/api/v1/chats/message", "abc"))
                .thenReturn(Optional.of(saved));

        var result = idempotencyService.claim(10L, "POST:/api/v1/chats/message", "abc", new DummyRequest("hello"));

        assertEquals(IdempotencyService.ClaimAction.PROCEED, result.getAction());
        assertEquals(saved, result.getRecord());
        verify(idempotencyRecordRepository).insertPendingIfAbsent(any(), any(), any(), any(), any());
    }

    @Test
    void cleanupExpiredRecordsDeletesOldRows() {
        when(idempotencyRecordRepository.deleteByExpiresAtBefore(any(LocalDateTime.class))).thenReturn(3L);

        long deleted = idempotencyService.cleanupExpiredRecords();

        assertEquals(3L, deleted);
        verify(idempotencyRecordRepository).deleteByExpiresAtBefore(any(LocalDateTime.class));
    }

    @Test
    void claimReturnsCachedRecordWhenSucceeded() {
        when(idempotencyRecordRepository.insertPendingIfAbsent(any(), any(), any(), any(), any())).thenReturn(0);

        String endpoint = "POST:/api/v1/chats/message";
        String key = "abc";
        DummyRequest request = new DummyRequest("hello");
        String hash = idempotencyService.hashRequestForTest(request);
        IdempotencyRecord existing = IdempotencyRecord.builder()
                .id(1L)
                .userId(10L)
                .endpoint(endpoint)
                .idempotencyKey(key)
                .requestHash(hash)
                .status(IdempotencyStatus.SUCCEEDED)
                .responseBody("cached")
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        when(idempotencyRecordRepository.findForUpdate(10L, endpoint, key)).thenReturn(Optional.of(existing));

        var result = idempotencyService.claim(10L, endpoint, key, request);

        assertEquals(IdempotencyService.ClaimAction.RETURN_CACHED, result.getAction());
        assertEquals("cached", result.getRecord().getResponseBody());
    }

    @Test
    void claimRejectsSameKeyWithDifferentPayload() {
        when(idempotencyRecordRepository.insertPendingIfAbsent(any(), any(), any(), any(), any())).thenReturn(0);

        IdempotencyRecord existing = IdempotencyRecord.builder()
                .id(1L)
                .userId(10L)
                .endpoint("POST:/api/v1/chats/message")
                .idempotencyKey("abc")
                .requestHash(idempotencyService.hashRequestForTest(new DummyRequest("hello")))
                .status(IdempotencyStatus.SUCCEEDED)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        when(idempotencyRecordRepository.findForUpdate(10L, "POST:/api/v1/chats/message", "abc"))
                .thenReturn(Optional.of(existing));

        assertThrows(ErrorHandler.class, () ->
                idempotencyService.claim(10L, "POST:/api/v1/chats/message", "abc", new DummyRequest("other")));
    }

    @Test
    void claimRetriesFailedRetryableRecord() {
        when(idempotencyRecordRepository.insertPendingIfAbsent(any(), any(), any(), any(), any())).thenReturn(0);

        String endpoint = "POST:/api/v1/chats/message";
        String key = "abc";
        DummyRequest request = new DummyRequest("hello");
        IdempotencyRecord existing = IdempotencyRecord.builder()
                .id(1L)
                .userId(10L)
                .endpoint(endpoint)
                .idempotencyKey(key)
                .requestHash(idempotencyService.hashRequestForTest(request))
                .status(IdempotencyStatus.FAILED_RETRYABLE)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        when(idempotencyRecordRepository.findForUpdate(10L, endpoint, key)).thenReturn(Optional.of(existing));

        var result = idempotencyService.claim(10L, endpoint, key, request);

        assertEquals(IdempotencyService.ClaimAction.PROCEED, result.getAction());
        assertEquals(IdempotencyStatus.PENDING, existing.getStatus());
    }

    @Test
    void claimReusesExpiredRecordAsNewAttempt() {
        when(idempotencyRecordRepository.insertPendingIfAbsent(any(), any(), any(), any(), any())).thenReturn(0);

        String endpoint = "POST:/api/v1/chats/message";
        String key = "abc";
        DummyRequest request = new DummyRequest("hello");
        IdempotencyRecord existing = IdempotencyRecord.builder()
                .id(1L)
                .userId(10L)
                .endpoint(endpoint)
                .idempotencyKey(key)
                .requestHash("old-hash")
                .status(IdempotencyStatus.SUCCEEDED)
                .responseBody("old")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        when(idempotencyRecordRepository.findForUpdate(10L, endpoint, key)).thenReturn(Optional.of(existing));

        var result = idempotencyService.claim(10L, endpoint, key, request);

        assertEquals(IdempotencyService.ClaimAction.PROCEED, result.getAction());
        assertEquals(IdempotencyStatus.PENDING, existing.getStatus());
        assertEquals(idempotencyService.hashRequestForTest(request), existing.getRequestHash());
    }

    @Test
    void serializeSupportsLocalDateTimePayload() {
        String serialized = idempotencyService.serialize(new LocalDateTimeResponse(LocalDateTime.of(2026, 3, 17, 14, 10)));

        assertEquals("{\"createdAt\":[2026,3,17,14,10]}", serialized);
    }

    private record DummyRequest(String content) {
    }

    private record LocalDateTimeResponse(LocalDateTime createdAt) {
    }
}
