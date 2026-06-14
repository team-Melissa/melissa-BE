package com.melissa.diary.service;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.domain.Entitlement;
import com.melissa.diary.domain.User;
import com.melissa.diary.domain.enums.EntitlementSourceType;
import com.melissa.diary.domain.enums.EntitlementType;
import com.melissa.diary.domain.enums.PaymentPlatform;
import com.melissa.diary.repository.EntitlementRepository;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.web.dto.EntitlementResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntitlementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EntitlementRepository entitlementRepository;

    @InjectMocks
    private EntitlementService entitlementService;

    @Test
    void getUserEntitlementsReturnsEmptyFeaturesWhenNoActiveEntitlements() {
        Long userId = 1L;
        User user = User.builder()
                .id(userId)
                .provider("GOOGLE")
                .nickname("user")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(entitlementRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of());

        EntitlementResponseDTO.EntitlementsResponse response = entitlementService.getUserEntitlements(userId);

        assertThat(response.getEntitlements()).isEmpty();
        assertThat(response.getFeatures().getAdRemoved()).isFalse();
    }

    @Test
    void getUserEntitlementsMapsRemoveAdsToAdRemovedFeature() {
        Long userId = 1L;
        LocalDateTime grantedAt = LocalDateTime.of(2026, 6, 15, 10, 0);
        User user = User.builder()
                .id(userId)
                .provider("APPLE")
                .nickname("user")
                .build();
        Entitlement entitlement = Entitlement.builder()
                .user(user)
                .entitlementType(EntitlementType.REMOVE_ADS)
                .active(true)
                .sourceType(EntitlementSourceType.PAYMENT)
                .sourcePlatform(PaymentPlatform.APPLE)
                .grantedAt(grantedAt)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(entitlementRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(entitlement));

        EntitlementResponseDTO.EntitlementsResponse response = entitlementService.getUserEntitlements(userId);

        assertThat(response.getFeatures().getAdRemoved()).isTrue();
        assertThat(response.getEntitlements()).hasSize(1);
        assertThat(response.getEntitlements().get(0).getType()).isEqualTo("REMOVE_ADS");
        assertThat(response.getEntitlements().get(0).getActive()).isTrue();
        assertThat(response.getEntitlements().get(0).getSourcePlatform()).isEqualTo("APPLE");
        assertThat(response.getEntitlements().get(0).getGrantedAt()).isEqualTo(grantedAt);
    }

    @Test
    void getUserEntitlementsThrowsWhenUserDoesNotExist() {
        Long userId = 404L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ErrorHandler error = assertThrows(ErrorHandler.class, () -> entitlementService.getUserEntitlements(userId));

        assertThat(error.getErrorCode()).isEqualTo(ErrorStatus.USER_NOT_FOUND);
    }
}
