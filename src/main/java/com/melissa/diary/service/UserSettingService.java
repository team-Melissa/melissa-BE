package com.melissa.diary.service;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.converter.UserSettingConverter;
import com.melissa.diary.domain.UserSetting;
import com.melissa.diary.repository.AiProfileRepository;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.repository.UserSettingRepository;
import com.melissa.diary.web.dto.UserSettingRequestDTO;
import com.melissa.diary.web.dto.UserSettingResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserSettingService {
    private final UserSettingRepository userSettingRepository;
    private final AiProfileRepository aiProfileRepository;
    private final UserRepository userRepository;


    // 조회
    public UserSettingResponseDTO.UserSettingResponse getUserSettings(Long userId) {
        UserSetting userSetting = userSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.SETTING_NOT_FOUND));

        return UserSettingConverter.toResponse(userSetting);
    }

    // 수정
    public UserSettingResponseDTO.UserSettingResponse updateUserSettings(Long userId, UserSettingRequestDTO.UserSettingRequest request) {
        UserSetting existingSetting = userSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.SETTING_NOT_FOUND));

        UserSetting updated = UserSettingConverter.updateEntity(request, existingSetting);
        userSettingRepository.save(updated);

        return UserSettingConverter.toResponse(updated);
    }
}
