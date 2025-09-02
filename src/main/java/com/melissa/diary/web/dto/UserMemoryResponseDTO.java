package com.melissa.diary.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class UserMemoryResponseDTO {
    
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemoryResponse {
        private String memoryContent;
        private LocalDateTime lastUpdatedAt;
        private boolean hasMemory;
    }
    
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemoryResetResponse {
        private String message;
        private LocalDateTime resetAt;
    }
}
