package com.melissa.diary.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class CalenderResponseDTO {
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
