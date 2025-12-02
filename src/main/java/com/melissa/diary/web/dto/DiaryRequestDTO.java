package com.melissa.diary.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class DiaryRequestDTO {
    
    /**
     * 수동 일기 작성 요청 (v1.3.0)
     * - 해시태그는 LLM 자동 생성
     * - 이미지는 DALL-E로 생성 (요청에서 받지 않음)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "수동 일기 작성 요청")
    public static class ManualDiaryCreateRequest {
        
        @NotNull(message = "AI 프로필 ID는 필수입니다.")
        @Schema(description = "AI 프로필 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long aiProfileId;
        
        @NotNull(message = "년도는 필수입니다.")
        @Min(value = 2020, message = "년도는 2020 이상이어야 합니다.")
        @Max(value = 2100, message = "년도는 2100 이하여야 합니다.")
        @Schema(description = "년도", example = "2025", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer year;
        
        @NotNull(message = "월은 필수입니다.")
        @Min(value = 1, message = "월은 1 이상이어야 합니다.")
        @Max(value = 12, message = "월은 12 이하여야 합니다.")
        @Schema(description = "월", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer month;
        
        @NotNull(message = "일은 필수입니다.")
        @Min(value = 1, message = "일은 1 이상이어야 합니다.")
        @Max(value = 31, message = "일은 31 이하여야 합니다.")
        @Schema(description = "일", example = "15", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer day;
        
        @Size(max = 100, message = "제목은 최대 100자까지 입력 가능합니다.")
        @Schema(description = "일기 제목", example = "오늘의 일기", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String title;
        
        @NotNull(message = "내용은 필수입니다.")
        @Size(min = 1, max = 5000, message = "내용은 1자 이상 5000자 이하여야 합니다.")
        @Schema(description = "일기 내용", example = "오늘은 좋은 하루였다.", requiredMode = Schema.RequiredMode.REQUIRED)
        private String content;
        
        @Size(max = 40, message = "기분은 최대 40자까지 입력 가능합니다.")
        @Schema(description = "기분", example = "행복", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String mood;
        
        // 해시태그는 LLM이 자동 생성 (사용자 입력 X)
        
        @Builder.Default
        @Schema(description = "이미지 생성 여부 (기본값 true)", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        private Boolean generateImage = true;
    }
    
    /**
     * 일기 수정 요청
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "일기 수정 요청 (null이 아닌 필드만 업데이트)")
    public static class DiaryUpdateRequest {
        
        @Size(max = 100, message = "제목은 최대 100자까지 입력 가능합니다.")
        @Schema(description = "일기 제목", example = "수정된 제목", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String title;
        
        @Size(max = 5000, message = "내용은 최대 5000자까지 입력 가능합니다.")
        @Schema(description = "일기 내용", example = "수정된 내용", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String content;
        
        @Size(max = 40, message = "기분은 최대 40자까지 입력 가능합니다.")
        @Schema(description = "기분", example = "평온", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String mood;
        
        @Size(max = 30, message = "해시태그1은 최대 30자까지 입력 가능합니다.")
        @Schema(description = "해시태그 1", example = "#수정", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String hashtag1;
        
        @Size(max = 30, message = "해시태그2는 최대 30자까지 입력 가능합니다.")
        @Schema(description = "해시태그 2", example = "#일기", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String hashtag2;
        
        @Builder.Default
        @Schema(description = "이미지 재생성 여부 (기본값 false)", example = "false", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        private Boolean generateImage = false;
    }
    
    /**
     * 채팅 기반 일기 생성 요청 (v1.3.0)
     * - Thread의 채팅 로그를 LLM으로 요약
     * - 제목, 내용, 해시태그 자동 생성
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "채팅 기반 일기 생성 요청")
    public static class ChatDiaryCreateRequest {
        
        @NotNull(message = "AI 프로필 ID는 필수입니다.")
        @Schema(description = "AI 프로필 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long aiProfileId;
        
        @NotNull(message = "년도는 필수입니다.")
        @Min(value = 2020, message = "년도는 2020 이상이어야 합니다.")
        @Max(value = 2100, message = "년도는 2100 이하여야 합니다.")
        @Schema(description = "년도", example = "2025", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer year;
        
        @NotNull(message = "월은 필수입니다.")
        @Min(value = 1, message = "월은 1 이상이어야 합니다.")
        @Max(value = 12, message = "월은 12 이하여야 합니다.")
        @Schema(description = "월", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer month;
        
        @NotNull(message = "일은 필수입니다.")
        @Min(value = 1, message = "일은 1 이상이어야 합니다.")
        @Max(value = 31, message = "일은 31 이하여야 합니다.")
        @Schema(description = "일", example = "15", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer day;
        
        @Builder.Default
        @Schema(description = "이미지 생성 여부 (기본값 true)", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        private Boolean generateImage = true;
    }
}

