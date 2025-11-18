package com.melissa.diary.web.dto;

import com.melissa.diary.domain.enums.Platform;
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
    public static class TokenResponse {
        private Long id;
        private String expoPushToken;
        private Platform platform;
        private String deviceId;
        private Boolean invalid;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeleteResponse {
        private String expoPushToken;
        private String message;
    }
}

