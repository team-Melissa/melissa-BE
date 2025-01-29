package com.melissa.diary.converter;

import com.melissa.diary.web.dto.CalenderResponseDTO;
import com.melissa.diary.domain.Thread;

public class ThreadConverter {
    public static CalenderResponseDTO.dailySummaryResponseDTO toDailySummaryResponseDTO(Thread thread) {
        return CalenderResponseDTO.dailySummaryResponseDTO.builder()
                .year(thread.getYear())
                .month(thread.getMonth())
                .day(thread.getDay())
                .summaryTitle(thread.getSummaryTitle())
                .summaryMood(thread.getMood() != null ? thread.getMood().name() : null)
                .summaryContent(thread.getSummaryContent())
                .imageS3(thread.getMoodImage())
                .build();
    }

    public static CalenderResponseDTO.dailyResponseDTO toDailyResponseDTO(Thread thread) {
        return CalenderResponseDTO.dailyResponseDTO.builder()
                .year(thread.getYear())
                .month(thread.getMonth())
                .day(thread.getDay())
                .hashTag1(thread.getHashtag1())
                .hashTag2(thread.getHashtag2())
                .imageS3(thread.getMoodImage())
                .build();
    }
}
