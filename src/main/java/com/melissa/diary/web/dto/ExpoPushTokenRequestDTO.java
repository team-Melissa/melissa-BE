package com.melissa.diary.web.dto;

import com.melissa.diary.domain.enums.Platform;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class ExpoPushTokenRequestDTO {
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterTokenRequest {
        
        @NotBlank(message = "Expo Push Token은 필수입니다.")
        @Pattern(regexp = "^ExponentPushToken\\[.+\\]$", 
                 message = "유효한 Expo Push Token 형식이 아닙니다. (ExponentPushToken[...])")
        @Schema(description = "Expo Push Token. 형식: ExponentPushToken[...]",
                example = "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]",
                requiredMode = Schema.RequiredMode.REQUIRED)
        private String expoPushToken;
        
        @NotNull(message = "플랫폼 정보는 필수입니다.")
        @Schema(description = "클라이언트 플랫폼. 후보: ANDROID, IOS",
                example = "ANDROID",
                allowableValues = {"ANDROID", "IOS"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        private Platform platform;
        
        @Schema(description = "기기 ID. 앱에서 관리하는 디바이스 식별자가 있으면 전달하며, 없으면 생략 가능",
                example = "device-uuid-1234",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                nullable = true)
        private String deviceId;
    }
}

