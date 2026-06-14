package com.melissa.diary.service;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.domain.User;
import com.melissa.diary.domain.enums.AccountRole;
import com.melissa.diary.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthorizationServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminAuthorizationService adminAuthorizationService;

    @Test
    void assertAdminAllowsAdminUser() {
        Long userId = 1L;
        User admin = User.builder()
                .id(userId)
                .provider("GOOGLE")
                .nickname("admin")
                .accountRole(AccountRole.ADMIN)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(admin));

        assertDoesNotThrow(() -> adminAuthorizationService.assertAdmin(userId));
    }

    @Test
    void assertAdminRejectsNormalUser() {
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .provider("GOOGLE")
                .nickname("user")
                .accountRole(AccountRole.USER)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        ErrorHandler error = assertThrows(ErrorHandler.class, () -> adminAuthorizationService.assertAdmin(userId));

        assertThat(error.getErrorCode()).isEqualTo(ErrorStatus.PAYMENT_ADMIN_REQUIRED);
    }
}
