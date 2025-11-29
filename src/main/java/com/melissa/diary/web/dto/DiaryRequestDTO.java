package com.melissa.diary.web.dto;

import com.melissa.diary.domain.enums.Mood;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class DiaryRequestDTO {
    
    /**
     * 수동 일기 작성 요청 (v1.3.0)
     * - 해시태그는 사용자 직접 입력 (LLM 아님)
     * - 이미지는 DALL-E로 생성 (요청에서 받지 않음)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManualDiaryCreateRequest {
        
        @NotNull(message = "AI 프로필 ID는 필수입니다.")
        private Long aiProfileId;
        
        @NotNull(message = "년도는 필수입니다.")
        @Min(value = 2020, message = "년도는 2020 이상이어야 합니다.")
        @Max(value = 2100, message = "년도는 2100 이하여야 합니다.")
        private Integer year;
        
        @NotNull(message = "월은 필수입니다.")
        @Min(value = 1, message = "월은 1 이상이어야 합니다.")
        @Max(value = 12, message = "월은 12 이하여야 합니다.")
        private Integer month;
        
        @NotNull(message = "일은 필수입니다.")
        @Min(value = 1, message = "일은 1 이상이어야 합니다.")
        @Max(value = 31, message = "일은 31 이하여야 합니다.")
        private Integer day;
        
        @Size(max = 100, message = "제목은 최대 100자까지 입력 가능합니다.")
        private String title;
        
        @NotNull(message = "내용은 필수입니다.")
        @Size(min = 1, max = 5000, message = "내용은 1자 이상 5000자 이하여야 합니다.")
        private String content;
        
        @Size(max = 40, message = "기분은 최대 40자까지 입력 가능합니다.")
        private String mood;
        
        @Size(max = 30, message = "해시태그1은 최대 30자까지 입력 가능합니다.")
        private String hashtag1;
        
        @Size(max = 30, message = "해시태그2는 최대 30자까지 입력 가능합니다.")
        private String hashtag2;
        
        // 이미지 생성 여부 (기본값 true)
        private Boolean generateImage = true;
    }
    
    /**
     * 일기 수정 요청
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiaryUpdateRequest {
        
        @Size(max = 100, message = "제목은 최대 100자까지 입력 가능합니다.")
        private String title;
        
        @Size(max = 5000, message = "내용은 최대 5000자까지 입력 가능합니다.")
        private String content;
        
        @Size(max = 40, message = "기분은 최대 40자까지 입력 가능합니다.")
        private String mood;
        
        @Size(max = 30, message = "해시태그1은 최대 30자까지 입력 가능합니다.")
        private String hashtag1;
        
        @Size(max = 30, message = "해시태그2는 최대 30자까지 입력 가능합니다.")
        private String hashtag2;
        
        // 이미지 재생성 여부 (기본값 false)
        private Boolean generateImage = false;
    }
}

