package com.melissa.diary.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

public class UserSettingResponseDTO {

    @Getter
    @Builder
    @Schema(description = "사용자 설정 응답")
    public static class UserSettingResponse {

        @Schema(description = "수면 시간 (기본값: 04:30)", example = "04:30", requiredMode = Schema.RequiredMode.REQUIRED)
        private String sleepTime;

        @Schema(description = "알림 시간 (기본값: 23:00)", example = "23:00", requiredMode = Schema.RequiredMode.REQUIRED)
        private String notificationTime;

        @Schema(description = "요약 알림 활성화 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        private boolean notificationSummary;
    }
}
