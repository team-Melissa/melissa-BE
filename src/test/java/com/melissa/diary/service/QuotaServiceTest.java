package com.melissa.diary.service;


import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.domain.User;
import com.melissa.diary.domain.enums.UsageCost;
import com.melissa.diary.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class QuotaServiceTest {

    private UserRepository userRepo;
    private QuotaService quotaService;

    @BeforeEach
    void setUp() {
        userRepo = Mockito.mock(UserRepository.class);      // 가짜 레포지토리
        quotaService = new QuotaService(userRepo);
    }

    @Test
    void 정상_차감() {
        User user = new User();
        user.setDailyQuota(100);
        user.setQuotaDate(java.time.LocalDate.now());
        when(userRepo.findById(anyLong())).thenReturn(Optional.of(user));

        quotaService.checkAndConsume(user, UsageCost.CHAT);   // 3 소비

        assertEquals(97, user.getDailyQuota());
    }

    @Test
    void 한도_초과_예외() {
        // given
        User user = new User();
        user.setId(2L);
        user.setDailyQuota(0);
        user.setQuotaDate(java.time.LocalDate.now());
        when(userRepo.findById(anyLong())).thenReturn(Optional.of(user));

        // when + then
        ErrorHandler ex = assertThrows(ErrorHandler.class,
                () -> quotaService.checkAndConsume(user, UsageCost.CHAT));

        assertEquals(ErrorStatus.QUOTA_LIMIT_EXCEEDED, ex.getErrorCode());
    }
}