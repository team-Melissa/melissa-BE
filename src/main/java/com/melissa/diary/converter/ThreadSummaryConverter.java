package com.melissa.diary.converter;

import com.melissa.diary.domain.Thread;
import com.melissa.diary.web.dto.ThreadResponseDTO;
import com.melissa.diary.web.dto.ThreadSummaryResponseDTO;

public class ThreadSummaryConverter {
    public static ThreadSummaryResponseDTO.dailySummaryResponseDTO toSummaryDTO(Thread thread){
        return ThreadSummaryResponseDTO.dailySummaryResponseDTO.builder()
                .year(thread.getYear())
                .month(thread.getMonth())
                .day(thread.getDay())
                .summaryTitle(thread.getSummaryTitle())
                .summaryMood(thread.getMood() != null ? thread.getMood().name() : null)
                .summaryContent(thread.getSummaryContent())
                .hashTag1(thread.getHashtag1())
                .hashTag2(thread.getHashtag2())
                .imageS3(thread.getImageUrl())
                .build();

    }
}
