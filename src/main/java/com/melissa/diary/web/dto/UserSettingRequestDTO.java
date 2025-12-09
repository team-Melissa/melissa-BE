package com.melissa.diary.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

public class UserSettingRequestDTO {
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "사용자 설정 수정 요청")
    public static class UserSettingRequest {

        @NotBlank(message = "수면 시간은 필수입니다.")
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "수면 시간은 HH:mm 형식이어야 합니다.")
        @Schema(description = "수면 시간 (HH:mm)", example = "23:00", requiredMode = Schema.RequiredMode.REQUIRED)
        private String sleepTime;

        @NotBlank(message = "알림 시간은 필수입니다.")
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "알림 시간은 HH:mm 형식이어야 합니다.")
        @Schema(description = "알림 시간 (HH:mm)", example = "21:00", requiredMode = Schema.RequiredMode.REQUIRED)
        private String notificationTime;

        @NotNull(message = "알림 활성화 여부는 필수입니다.")
        @Schema(description = "알림 활성화 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        private Boolean notificationEnabled;
    }
}
