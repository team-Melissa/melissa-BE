package com.melissa.diary.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

public class UserSettingRequestDTO {
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "사용자 설정 수정 요청")
    public static class UserSettingRequest {

        @Schema(description = "수면 시간 (HH:mm 형식)", example = "23:00", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String sleepTime;

        @Schema(description = "알림 시간 (HH:mm 형식)", example = "21:00", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String notificationTime;

        @Schema(description = "요약 알림 활성화 여부", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        private boolean notificationSummary;
    }
}
