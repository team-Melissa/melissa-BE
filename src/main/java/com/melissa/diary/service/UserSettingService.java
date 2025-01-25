package com.melissa.diary.service;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.converter.UserSettingConverter;
import com.melissa.diary.domain.User;
import com.melissa.diary.domain.UserSetting;
import com.melissa.diary.repository.AiProfileRepository;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.repository.UserSettingRepository;
import com.melissa.diary.web.dto.UserSettingRequestDTO;
import com.melissa.diary.web.dto.UserSettingResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Time;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserSettingService {
    private final UserSettingRepository userSettingRepository;
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

    public void createDefaultSetting(Long userId) {
        // 이미 존재하면 등록하지 않음 (필요시 로직 조정)
        Optional<UserSetting> optional = userSettingRepository.findByUserId(userId);
        if (optional.isPresent()) { // 이미 존재할 때, 기본값 등록을 하려고하는 경우
            new ErrorHandler(ErrorStatus.SETTING_ALREADY_ENROLL);
        }

        // 유저 찾기
        User user = userRepository.findById(userId)
                .orElseThrow(() ->  new ErrorHandler(ErrorStatus.SETTING_NOT_FOUND));

        // 기본값 설정
        UserSetting defaultSetting = UserSetting.builder()
                .user(user)
                .sleepTime(Time.valueOf("04:30:00"))
                .notificationTime(Time.valueOf("23:00:00"))
                .notificationSummary(true)
                .notificationQna(true)
                .build();

        userSettingRepository.save(defaultSetting);
    }

    public boolean isNewUser(Long userId) {
        // 설정값이 있으면 신규가입이 아님 (기본값이라도 있으면, 기존유저)
        return !userSettingRepository.existsByUserId(userId);
    }
}
