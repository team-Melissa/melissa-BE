package com.melissa.diary.web.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class DiaryRequestDTO {
    
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
        
        @Size(max = 30, message = "해시태그는 최대 30자까지 입력 가능합니다.")
        private String hashtag1;
        
        @Size(max = 30, message = "해시태그는 최대 30자까지 입력 가능합니다.")
        private String hashtag2;
        
        @Size(max = 255, message = "이미지 URL은 최대 255자까지 입력 가능합니다.")
        private String imageUrl;
    }
}

