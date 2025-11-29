package com.melissa.diary.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class DiaryResponseDTO {
    
    /**
     * 일기 삭제 응답
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiaryDeleteResponse {
        private Long diaryId;
        private String message;
    }
    
    /**
     * 일기 생성/수정 응답
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiaryResponse {
        private Long diaryId;
        private Long threadId;
        private int year;
        private int month;
        private int day;
        private String title;
        private String content;
        private String mood;
        private String hashtag1;
        private String hashtag2;
        private String imageUrl;
        private int version;
        private LocalDateTime createdAt;
    }
}

