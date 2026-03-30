package com.melissa.diary.converter;

import com.melissa.diary.aws.s3.S3AssetUrlResolver;
import com.melissa.diary.domain.Diary;
import com.melissa.diary.web.dto.CalendarResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Diary 엔티티를 DTO로 변환하는 컨버터
 */
@Component
@RequiredArgsConstructor
public class DiaryConverter {

    private final S3AssetUrlResolver s3AssetUrlResolver;
    
    /**
     * Diary -> DiaryDetailDTO 변환 (상세 정보)
     */
    public CalendarResponseDTO.DiaryDetailDTO toDiaryDetailDTO(Diary diary) {
        return CalendarResponseDTO.DiaryDetailDTO.builder()
                .diaryId(diary.getId())
                .aiProfileId(diary.getThread().getAiProfile().getId())
                .title(diary.getTitle())
                .content(diary.getContent())
                .mood(diary.getMood() != null ? diary.getMood().name() : null)
                .type(diary.getType().name())
                .hashtag1(diary.getHashtag1())
                .hashtag2(diary.getHashtag2())
                .imageUrl(s3AssetUrlResolver.resolve(diary.getImageUrl()))
                .imageStatus(diary.getImageStatus() != null ? diary.getImageStatus().name() : null)
                .version(diary.getVersion())
                .createdAt(diary.getCreatedAt())
                .build();
    }
    
    /**
     * Diary -> DiaryPreviewDTO 변환 (미리보기)
     */
    public CalendarResponseDTO.DiaryPreviewDTO toDiaryPreviewDTO(Diary diary) {
        return CalendarResponseDTO.DiaryPreviewDTO.builder()
                .diaryId(diary.getId())
                .aiProfileId(diary.getThread().getAiProfile().getId())
                .type(diary.getType().name())
                .hashtag1(diary.getHashtag1())
                .hashtag2(diary.getHashtag2())
                .imageUrl(s3AssetUrlResolver.resolve(diary.getImageUrl()))
                .imageStatus(diary.getImageStatus() != null ? diary.getImageStatus().name() : null)
                .build();
    }
}

