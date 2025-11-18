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
        @Schema(description = "Expo Push Token", 
                example = "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]",
                required = true)
        private String expoPushToken;
        
        @NotNull(message = "플랫폼 정보는 필수입니다.")
        @Schema(description = "플랫폼 (ANDROID 또는 IOS)", 
                example = "ANDROID",
                allowableValues = {"ANDROID", "IOS"},
                required = true)
        private Platform platform;
        
        @Schema(description = "기기 ID (선택사항) nullable", 
                example = "device-uuid-1234")
        private String deviceId;
    }
}

