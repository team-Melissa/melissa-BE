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
    public List<AiProfileResponseDTO.AiProfileResponse> getDefaultProfileList() {
        List<DefaultAiProfile> list = defaultAiProfileRepository.findAll();
        return list.stream()
                .map(AiProfileConverter::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AiProfileResponseDTO.AiProfileResponse getDefaultProfileOrThrow(Long id) {
        DefaultAiProfile profile = defaultAiProfileRepository.findById(id)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.PROFILE_NOT_FOUND));
        return AiProfileConverter.toResponse(profile);
    }

    @Transactional
    public void deleteUserDefaultProfileOrThrow(Long userId, Long defaultProfileId) {
        UserDefaultAiProfileMapping mapping = mappingRepository.findByUserIdAndDefaultAiProfileId(userId, defaultProfileId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.PROFILE_NOT_FOUND));
        mapping.setActive(false);
        mappingRepository.save(mapping);
    }

    /**
     * 유저가 가지고 있는 매핑 수와 전체 기본 제공 프로필 수가 다르면 누락된 매핑을 자동 생성한다.
     */
    @Transactional
    public void syncUserDefaultProfileMappings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));
        var allProfiles = defaultAiProfileRepository.findAll();
        var userMappings = mappingRepository.findAll().stream()
                .filter(m -> m.getUser().getId().equals(userId))
                .map(m -> m.getDefaultAiProfile().getId())
                .collect(java.util.stream.Collectors.toSet());
        for (DefaultAiProfile profile : allProfiles) {
            if (!userMappings.contains(profile.getId())) {
                UserDefaultAiProfileMapping mapping = new UserDefaultAiProfileMapping();
                mapping.setUser(user);
                mapping.setDefaultAiProfile(profile);
                mapping.setActive(true);
                mappingRepository.save(mapping);
            }
        }
    }
} 