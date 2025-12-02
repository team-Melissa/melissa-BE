package com.melissa.diary.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "사용자 메모리 응답")
    public static class MemoryResponse {
        
        @Schema(description = "메모리 내용", example = "사용자는 커피를 좋아합니다.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String memoryContent;
        
        @Schema(description = "마지막 수정일시", example = "2025-01-01T12:00:00", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private LocalDateTime lastUpdatedAt;
        
        @Schema(description = "메모리 존재 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        private boolean hasMemory;
    }
    
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "사용자 메모리 초기화 응답")
    public static class MemoryResetResponse {
        
        @Schema(description = "결과 메시지", example = "메모리가 초기화되었습니다.", requiredMode = Schema.RequiredMode.REQUIRED)
        private String message;
        
        @Schema(description = "초기화 일시", example = "2025-01-01T12:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDateTime resetAt;
    }
}
