package com.melissa.diary.web.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

public class UserSettingRequestDTO {
    @Getter
    @Builder
    public static class UserSettingRequest {

        private String sleepTime;

        private String notificationTime;

        private boolean notificationSummary;
    }

}
