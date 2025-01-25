package com.melissa.diary.web.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

public class UserSettingResponseDTO {

    @Getter
    @Builder
    public static class UserSettingResponse {

        private String sleepTime;

        private String notificationTime;

        private boolean notificationSummary;
    }
}
