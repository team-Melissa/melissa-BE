package com.melissa.diary.converter;

import com.melissa.diary.domain.Diary;
import com.melissa.diary.web.dto.CalenderResponseDTO;
import org.springframework.stereotype.Component;

/**
 * Diary 엔티티를 DTO로 변환하는 컨버터
 */
@Component
public class DiaryConverter {
    
    /**
     * Diary -> DiaryDetailDTO 변환 (상세 정보)
     */
    public static CalenderResponseDTO.DiaryDetailDTO toDiaryDetailDTO(Diary diary) {
        return CalenderResponseDTO.DiaryDetailDTO.builder()
                .diaryId(diary.getId())
                .title(diary.getTitle())
                .content(diary.getContent())
                .mood(diary.getMood() != null ? diary.getMood().name() : null)
                .type(diary.getType().name())  // 일기 생성 타입 추가
                .hashtag1(diary.getHashtag1())
                .hashtag2(diary.getHashtag2())
                .imageUrl(diary.getImageUrl())
                .version(diary.getVersion())
                .createdAt(diary.getCreatedAt())
                .build();
    }
    
    /**
     * Diary -> DiaryPreviewDTO 변환 (미리보기)
     */
    public static CalenderResponseDTO.DiaryPreviewDTO toDiaryPreviewDTO(Diary diary) {
        return CalenderResponseDTO.DiaryPreviewDTO.builder()
                .diaryId(diary.getId())
                .type(diary.getType().name())  // 일기 생성 타입 추가
                .hashtag1(diary.getHashtag1())
                .hashtag2(diary.getHashtag2())
                .imageUrl(diary.getImageUrl())
                .build();
    }
}

