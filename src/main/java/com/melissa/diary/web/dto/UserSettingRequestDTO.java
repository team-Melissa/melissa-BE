package com.melissa.diary.web.dto;

import lombok.*;

public class UserSettingRequestDTO {
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSettingRequest {

        private String sleepTime;

        private String notificationTime;

        private boolean notificationSummary;
    }

}
