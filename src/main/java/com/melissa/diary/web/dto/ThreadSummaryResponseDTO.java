package com.melissa.diary.web.dto;

import lombok.Builder;
import lombok.Getter;

public class ThreadSummaryResponseDTO {

    @Getter
    @Builder
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
}
