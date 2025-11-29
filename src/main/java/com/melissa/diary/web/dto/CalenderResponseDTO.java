package com.melissa.diary.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class CalenderResponseDTO {
    
    // ============== [v1.3.0] 새로운 DTO 구조 ==============
    
    /**
     * 단일 일기 상세 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiaryDetailDTO {
        private Long diaryId;
        private String title;
        private String content;
        private String mood;
        private String hashtag1;
        private String hashtag2;
        private String imageUrl;
        private int version;
        private LocalDateTime createdAt;
    }
    
    /**
     * 단일 일기 미리보기
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiaryPreviewDTO {
        private Long diaryId;
        private String hashtag1;
        private String hashtag2;
        private String imageUrl;
    }
    
    /**
     * 날짜별 일기 응답 (diaries 배열, 최대 3개)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailySummaryResponseDTO {
        private int year;
        private int month;
        private int day;
        private List<DiaryDetailDTO> diaries;
    }
    
    /**
     * 날짜별 미리보기 응답
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyPreviewResponseDTO {
        private int year;
        private int month;
        private int day;
        private List<DiaryPreviewDTO> diaries;
    }
    
    // ============== [Deprecated] 기존 DTO (Thread 기반) ==============
    
    @Deprecated
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class dailySummaryResponseDTO {
        private int year;
        private int month;
        private int day;
        private String summaryTitle;
        private String summaryMood;
        private String summaryContent;
        private String hashTag1;
        private String hashTag2;
        private String imageS3;
    }

    @Deprecated
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class dailyResponseDTO{
        private int year;
        private int month;
        private int day;
        private String hashTag1;
        private String hashTag2;
        private String imageS3;
    }
}
