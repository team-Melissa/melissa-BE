package com.melissa.diary.converter;

import com.melissa.diary.domain.UserSetting;
import com.melissa.diary.web.dto.UserSettingRequestDTO;
import com.melissa.diary.web.dto.UserSettingResponseDTO;

import java.sql.Time;

public class UserSettingConverter {
    // 유저 셋팅 객체 -> 응답으로 변환하는 컨버터
    public static UserSettingResponseDTO.UserSettingResponse toResponse(UserSetting userSetting) {
        return UserSettingResponseDTO.UserSettingResponse.builder()
                .sleepTime(userSetting.getSleepTime().toString())
                .notificationTime(userSetting.getNotificationTime().toString())
                .notificationSummary(userSetting.isNotificationSummary())
                .build();
    }

    // 기존의 유저 셋팅 객체와 request로 업데이트하는 컨버터
    public static UserSetting updateEntity(UserSettingRequestDTO.UserSettingRequest request, UserSetting existing) {
        existing.setSleepTime(Time.valueOf(request.getSleepTime()));
        existing.setNotificationTime(Time.valueOf(request.getNotificationTime()));
        existing.setNotificationSummary(request.isNotificationSummary());
        return existing;
    }

}
