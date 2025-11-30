package com.melissa.diary.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ThreadRequestDTO {
    
    /**
     * v1: 기본 채팅 요청
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiChatRequest {
        @NotNull(message = "AI 프로필 ID는 필수입니다.")
        @Schema(description = "AI 프로필 ID", example = "1")
        private Long aiProfileId;
        
        @NotBlank(message = "메시지 내용은 필수입니다.")
        @Size(min = 1, max = 1000, message = "메시지는 1자 이상 1000자 이하여야 합니다.")
        @Schema(description = "전송할 메시지 내용", example = "오늘 기분이 어때?")
        private String content;
        
        @Schema(description = "연도", example = "2025")
        private int year;
        
        @Schema(description = "월", example = "11")
        private int month;
        
        @Schema(description = "일", example = "30")
        private int day;
    }
    
    /**
     * v2: UserMemory 기반 채팅 요청
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiChatRequestV2 {
        @NotNull(message = "AI 프로필 ID는 필수입니다.")
        @Schema(description = "AI 프로필 ID", example = "1")
        private Long aiProfileId;
        
        @NotBlank(message = "메시지 내용은 필수입니다.")
        @Size(min = 1, max = 1000, message = "메시지는 1자 이상 1000자 이하여야 합니다.")
        @Schema(description = "전송할 메시지 내용", example = "오늘 기분이 어때?")
        private String content;
        
        @Schema(description = "연도", example = "2025")
        private int year;
        
        @Schema(description = "월", example = "11")
        private int month;
        
        @Schema(description = "일", example = "30")
        private int day;
    }
}
