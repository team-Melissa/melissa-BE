package com.melissa.diary.service;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.domain.Entitlement;
import com.melissa.diary.domain.enums.EntitlementType;
import com.melissa.diary.repository.EntitlementRepository;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.web.dto.EntitlementResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EntitlementService {

    private final UserRepository userRepository;
    private final EntitlementRepository entitlementRepository;

    @Transactional(readOnly = true)
    public EntitlementResponseDTO.EntitlementsResponse getUserEntitlements(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));

        List<Entitlement> activeEntitlements = entitlementRepository.findByUserIdAndActiveTrue(userId);
        boolean adRemoved = activeEntitlements.stream()
                .anyMatch(entitlement -> entitlement.getEntitlementType() == EntitlementType.REMOVE_ADS);

        return EntitlementResponseDTO.EntitlementsResponse.builder()
                .entitlements(activeEntitlements.stream()
                        .map(this::toSummary)
                        .toList())
                .features(EntitlementResponseDTO.FeatureSummary.builder()
                        .adRemoved(adRemoved)
                        .build())
                .build();
    }

    private EntitlementResponseDTO.EntitlementSummary toSummary(Entitlement entitlement) {
        return EntitlementResponseDTO.EntitlementSummary.builder()
                .type(entitlement.getEntitlementType().name())
                .active(entitlement.getActive())
                .sourcePlatform(entitlement.getSourcePlatform() == null ? null : entitlement.getSourcePlatform().name())
                .grantedAt(entitlement.getGrantedAt())
                .revokedAt(entitlement.getRevokedAt())
                .build();
    }
}
