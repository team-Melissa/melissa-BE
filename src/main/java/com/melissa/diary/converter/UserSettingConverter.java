package com.melissa.diary.converter;

import com.melissa.diary.domain.UserSetting;
import com.melissa.diary.web.dto.UserSettingRequestDTO;
import com.melissa.diary.web.dto.UserSettingResponseDTO;

import java.sql.Time;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class UserSettingConverter {
    private static final DateTimeFormatter REQUEST_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private static final DateTimeFormatter RESPONSE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // 유저 셋팅 객체 -> 응답으로 변환하는 컨버터
    public static UserSettingResponseDTO.UserSettingResponse toResponse(UserSetting userSetting) {
        return UserSettingResponseDTO.UserSettingResponse.builder()
                .sleepTime(formatTime(userSetting.getSleepTime()))
                .notificationTime(formatTime(userSetting.getNotificationTime()))
                .notificationEnabled(userSetting.isNotificationEnabled())
                .build();
    }

    // 기존의 유저 셋팅 객체와 request로 업데이트하는 컨버터
    public static UserSetting updateEntity(UserSettingRequestDTO.UserSettingRequest request, UserSetting existing) {
        existing.setSleepTime(parseTime(request.getSleepTime()));
        existing.setNotificationTime(parseTime(request.getNotificationTime()));
        existing.setNotificationEnabled(Boolean.TRUE.equals(request.getNotificationEnabled()));
        return existing;
    }

    private static Time parseTime(String value) {
        try {
            LocalTime localTime = LocalTime.parse(value, REQUEST_TIME_FORMATTER).withSecond(0);
            return Time.valueOf(localTime);
        } catch (DateTimeParseException e) {
            throw new com.melissa.diary.apiPayload.exception.handler.ErrorHandler(
                    com.melissa.diary.apiPayload.code.status.ErrorStatus.SETTING_INVALID_TIME_FORMAT
            );
        }
    }

    private static String formatTime(Time time) {
        return time.toLocalTime().format(RESPONSE_TIME_FORMATTER);
    }
}
