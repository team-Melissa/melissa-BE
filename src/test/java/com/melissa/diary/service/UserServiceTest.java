package com.melissa.diary.service;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.domain.User;
import com.melissa.diary.repository.PaymentRepository;
import com.melissa.diary.repository.ThreadRepository;
import com.melissa.diary.repository.UserMemoryRepository;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.repository.UserSettingRepository;
import com.melissa.diary.security.JwtProvider;
import com.melissa.diary.security.RefreshTokenHasher;
import com.melissa.diary.web.dto.UserResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SocialAuthService socialAuthService;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RefreshTokenHasher refreshTokenHasher;

    @Mock
    private ThreadRepository threadRepository;

    @Mock
    private UserSettingRepository userSettingRepository;

    @Mock
    private UserMemoryRepository userMemoryRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void deleteUserRejectsUserWithPaymentHistory() {
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .provider("GOOGLE")
                .providerId("google-1")
                .email("user@example.com")
                .nickname("user")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(paymentRepository.existsByUserId(userId)).thenReturn(true);

        ErrorHandler error = assertThrows(ErrorHandler.class, () -> userService.deleteUser(userId));

        assertThat(error.getErrorCode()).isEqualTo(ErrorStatus.PAYMENT_USER_DELETE_BLOCKED);
        verify(threadRepository, never()).deleteAllByUserId(userId);
        verify(userSettingRepository, never()).deleteByUserId(userId);
        verify(userMemoryRepository, never()).deleteByUserId(userId);
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void deleteUserDeletesUserWhenNoPaymentHistoryExists() {
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .provider("GOOGLE")
                .providerId("google-1")
                .email("user@example.com")
                .nickname("user")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(paymentRepository.existsByUserId(userId)).thenReturn(false);

        UserResponseDTO.DeleteResultDTO response = userService.deleteUser(userId);

        assertThat(response.getUserId()).isEqualTo(userId);
        verify(threadRepository).deleteAllByUserId(userId);
        verify(userSettingRepository).deleteByUserId(userId);
        verify(userMemoryRepository).deleteByUserId(userId);
        verify(userRepository).delete(user);
    }
}
