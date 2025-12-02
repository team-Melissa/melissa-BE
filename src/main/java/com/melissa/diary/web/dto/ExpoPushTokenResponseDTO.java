package com.melissa.diary.web.dto;

import com.melissa.diary.domain.enums.Platform;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class ExpoPushTokenResponseDTO {
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Expo Push Token 응답")
    public static class TokenResponse {
        
        @Schema(description = "토큰 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long id;
        
        @Schema(description = "Expo Push Token", example = "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]", requiredMode = Schema.RequiredMode.REQUIRED)
        private String expoPushToken;
        
        @Schema(description = "플랫폼", example = "ANDROID", requiredMode = Schema.RequiredMode.REQUIRED)
        private Platform platform;
        
        @Schema(description = "기기 ID", example = "device-uuid-1234", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String deviceId;
        
        @Schema(description = "유효하지 않은 토큰 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
        private Boolean invalid;
        
        @Schema(description = "생성일시", example = "2025-01-01T12:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDateTime createdAt;
        
        @Schema(description = "수정일시", example = "2025-01-01T12:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDateTime updatedAt;
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Expo Push Token 삭제 응답")
    public static class DeleteResponse {
        
        @Schema(description = "삭제된 Expo Push Token", example = "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]", requiredMode = Schema.RequiredMode.REQUIRED)
        private String expoPushToken;
        
        @Schema(description = "결과 메시지", example = "토큰이 삭제되었습니다.", requiredMode = Schema.RequiredMode.REQUIRED)
        private String message;
    }
}

