package com.melissa.diary.service;

import com.melissa.diary.converter.AiProfileConverter;
import com.melissa.diary.domain.DefaultAiProfile;
import com.melissa.diary.repository.DefaultAiProfileRepository;
import com.melissa.diary.repository.UserDefaultAiProfileMappingRepository;
import com.melissa.diary.domain.UserDefaultAiProfileMapping;
import com.melissa.diary.web.dto.AiProfileResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.domain.User;
import com.melissa.diary.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class DefaultAiProfileService {
    private final DefaultAiProfileRepository defaultAiProfileRepository;
    private final UserDefaultAiProfileMappingRepository mappingRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AiProfileResponseDTO.AiProfileResponse> getDefaultProfileList(Long userId) {
        List<UserDefaultAiProfileMapping> mappings = mappingRepository.findActiveMappingsWithProfileByUserId(userId);
        return mappings.stream()
                .map(m -> AiProfileConverter.toResponse(m.getDefaultAiProfile()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AiProfileResponseDTO.AiProfileResponse getDefaultProfileOrThrow(Long userId, Long defaultProfileId) {
        DefaultAiProfile profile = mappingRepository.findActiveProfile(userId, defaultProfileId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.PROFILE_NOT_FOUND));
        return AiProfileConverter.toResponse(profile);
    }

    @Transactional
    public void deleteUserDefaultProfileOrThrow(Long userId, Long defaultProfileId) {
        UserDefaultAiProfileMapping mapping = mappingRepository.findActiveMappingsWithProfileByUserId(userId).stream()
                .filter(m -> m.getDefaultAiProfile().getId().equals(defaultProfileId))
                .findFirst()
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.PROFILE_NOT_FOUND));
        mapping.setActive(false); // Dirty Checking으로 update
    }

    @Transactional
    public void restoreDefaultProfileOrThrow(Long userId){
        List<UserDefaultAiProfileMapping> mappings = mappingRepository.findByUserId(userId);
        for (UserDefaultAiProfileMapping mapping : mappings){
            if (!mapping.isActive()){
                mapping.setActive(true); // Dirty Checking으로 update
            }
        }
    }

    /**
     * 유저가 가지고 있는 매핑 수와 전체 기본 제공 프로필 수가 다르면 누락된 매핑을 자동 생성한다.
     */
    @Transactional
    public void syncUserDefaultProfileMappings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));
        var allProfiles = defaultAiProfileRepository.findAll();
        for (DefaultAiProfile profile : allProfiles) {
            if (!mappingRepository.existsByUserIdAndDefaultAiProfileId(userId, profile.getId())) {
                UserDefaultAiProfileMapping mapping = UserDefaultAiProfileMapping.builder()
                        .user(user)
                        .defaultAiProfile(profile)
                        .active(true)
                        .build();
                mappingRepository.save(mapping);
            }
        }
    }
} 