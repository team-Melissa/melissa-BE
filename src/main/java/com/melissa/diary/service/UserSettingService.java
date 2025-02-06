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
import org.springframework.transaction.annotation.Transactional;

import java.sql.Time;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserSettingService {
    private final UserSettingRepository userSettingRepository;
    private final UserRepository userRepository;


    // 조회
    @Transactional(readOnly = true)
    public UserSettingResponseDTO.UserSettingResponse getUserSettings(Long userId) {
        // db에 해당 유저 없으면 에러던지기(탈퇴 보호)
        User user = userRepository.findById(userId).orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));

        UserSetting userSetting = userSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.SETTING_NOT_FOUND));

        return UserSettingConverter.toResponse(userSetting);
    }


    // 수정
    @Transactional
    public UserSettingResponseDTO.UserSettingResponse updateUserSettings(Long userId, UserSettingRequestDTO.UserSettingRequest request) {
        // db에 해당 유저 없으면 에러던지기(탈퇴 보호)
        User user = userRepository.findById(userId).orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));

        UserSetting existingSetting = userSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.SETTING_NOT_FOUND));

        UserSetting updated = UserSettingConverter.updateEntity(request, existingSetting);
        userSettingRepository.save(updated);

        return UserSettingConverter.toResponse(updated);
    }

    @Transactional
    public void createDefaultSetting(Long userId) {

        // db에 해당 유저 없으면 에러던지기(탈퇴 보호)
        User user = userRepository.findById(userId).orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));

        // 이미 존재하면 등록하지 않음
        Optional<UserSetting> optional = userSettingRepository.findByUserId(userId);
        if (optional.isPresent()) {
            throw new ErrorHandler(ErrorStatus.SETTING_ALREADY_ENROLL);
        }

        // 기본값 설정
        UserSetting defaultSetting = UserSetting.builder()
                .user(user)
                .sleepTime(Time.valueOf("04:30:00"))
                .notificationTime(Time.valueOf("23:00:00"))
                .notificationSummary(true)
                .notificationQna(true)
                .build();

        // 예외 처리를 위해 try-catch로 감쌈
        try {
            userSettingRepository.save(defaultSetting);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // DB 유니크 제약 에러가 터지면, 우리가 원하는 커스텀 예외로 던진다.
            throw new ErrorHandler(ErrorStatus.SETTING_ALREADY_ENROLL);
        }
    }

    @Transactional(readOnly = true)
    public boolean isNewUser(Long userId) {
        // db에 해당 유저 없으면 에러던지기(탈퇴 보호)
        User user = userRepository.findById(userId).orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));

        // 설정값이 있으면 신규가입이 아님 (기본값이라도 있으면, 기존유저)
        return !userSettingRepository.existsByUserId(userId);
    }
}
