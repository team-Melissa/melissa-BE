package com.melissa.diary.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
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
    @Schema(description = "AI 채팅 요청")
    public static class AiChatRequest {
        
        @NotNull(message = "AI 프로필 ID는 필수입니다.")
        @Schema(description = "AI 프로필 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long aiProfileId;
        
        @NotBlank(message = "메시지 내용은 필수입니다.")
        @Size(min = 1, max = 1000, message = "메시지는 1자 이상 1000자 이하여야 합니다.")
        @Schema(description = "전송할 메시지 내용", example = "오늘 기분이 어때?", requiredMode = Schema.RequiredMode.REQUIRED)
        private String content;
        
        @NotNull(message = "년도는 필수입니다.")
        @Min(value = 2020, message = "년도는 2020 이상이어야 합니다.")
        @Max(value = 2100, message = "년도는 2100 이하여야 합니다.")
        @Schema(description = "연도", example = "2025", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer year;
        
        @NotNull(message = "월은 필수입니다.")
        @Min(value = 1, message = "월은 1 이상이어야 합니다.")
        @Max(value = 12, message = "월은 12 이하여야 합니다.")
        @Schema(description = "월", example = "11", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer month;
        
        @NotNull(message = "일은 필수입니다.")
        @Min(value = 1, message = "일은 1 이상이어야 합니다.")
        @Max(value = 31, message = "일은 31 이하여야 합니다.")
        @Schema(description = "일", example = "30", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer day;
    }
    
    /**
     * v2: UserMemory 기반 채팅 요청
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "메모리 기반 AI 채팅 요청")
    public static class AiChatRequestV2 {
        
        @NotNull(message = "AI 프로필 ID는 필수입니다.")
        @Schema(description = "AI 프로필 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long aiProfileId;
        
        @NotBlank(message = "메시지 내용은 필수입니다.")
        @Size(min = 1, max = 1000, message = "메시지는 1자 이상 1000자 이하여야 합니다.")
        @Schema(description = "전송할 메시지 내용", example = "오늘 기분이 어때?", requiredMode = Schema.RequiredMode.REQUIRED)
        private String content;
        
        @NotNull(message = "년도는 필수입니다.")
        @Min(value = 2020, message = "년도는 2020 이상이어야 합니다.")
        @Max(value = 2100, message = "년도는 2100 이하여야 합니다.")
        @Schema(description = "연도", example = "2025", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer year;
        
        @NotNull(message = "월은 필수입니다.")
        @Min(value = 1, message = "월은 1 이상이어야 합니다.")
        @Max(value = 12, message = "월은 12 이하여야 합니다.")
        @Schema(description = "월", example = "11", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer month;
        
        @NotNull(message = "일은 필수입니다.")
        @Min(value = 1, message = "일은 1 이상이어야 합니다.")
        @Max(value = 31, message = "일은 31 이하여야 합니다.")
        @Schema(description = "일", example = "30", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer day;
    }
}
