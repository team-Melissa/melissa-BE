package com.melissa.diary.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class CalendarResponseDTO {
    
    // ============== [v1.3.0] 새로운 DTO 구조 ==============
    
    /**
     * 단일 일기 상세 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "일기 상세 정보")
    public static class DiaryDetailDTO {
        
        @Schema(description = "일기 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long diaryId;
        
        @Schema(description = "AI 프로필 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long aiProfileId;
        
        @Schema(description = "일기 제목", example = "오늘의 일기", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String title;
        
        @Schema(description = "일기 내용", example = "오늘은 좋은 하루였다.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String content;
        
        @Schema(description = "기분 (기본값: HAPPY)", example = "HAPPY", 
                allowableValues = {"HAPPY", "SAD", "TIRED", "ANGRY", "RELAX"},
                requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String mood;
        
        @Schema(description = "일기 생성 타입 (MANUAL: 수동 작성, CHAT_BASED: 채팅 기반 자동 생성)", 
                example = "MANUAL", 
                allowableValues = {"MANUAL", "CHAT_BASED"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        private String type;
        
        @Schema(description = "해시태그 1", example = "#좋은하루", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String hashtag1;
        
        @Schema(description = "해시태그 2", example = "#감사", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String hashtag2;
        
        @Schema(description = "이미지 URL", example = "https://s3.amazonaws.com/...", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String imageUrl;
        
        @Schema(description = "버전", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private int version;
        
        @Schema(description = "생성일시", example = "2025-01-01T12:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDateTime createdAt;
    }
    
    /**
     * 단일 일기 미리보기
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "일기 미리보기")
    public static class DiaryPreviewDTO {
        
        @Schema(description = "일기 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long diaryId;
        
        @Schema(description = "AI 프로필 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long aiProfileId;
        
        @Schema(description = "일기 생성 타입 (MANUAL: 수동 작성, CHAT_BASED: 채팅 기반 자동 생성)", 
                example = "MANUAL", 
                allowableValues = {"MANUAL", "CHAT_BASED"},
                requiredMode = Schema.RequiredMode.REQUIRED)
        private String type;
        
        @Schema(description = "해시태그 1", example = "#좋은하루", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String hashtag1;
        
        @Schema(description = "해시태그 2", example = "#감사", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String hashtag2;
        
        @Schema(description = "이미지 URL", example = "https://s3.amazonaws.com/...", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String imageUrl;
    }
    
    /**
     * 날짜별 일기 응답 (diaries 배열, 최대 3개)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "날짜별 일기 상세 응답")
    public static class DailySummaryResponseDTO {
        
        @Schema(description = "년도", example = "2025", requiredMode = Schema.RequiredMode.REQUIRED)
        private int year;
        
        @Schema(description = "월", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private int month;
        
        @Schema(description = "일", example = "15", requiredMode = Schema.RequiredMode.REQUIRED)
        private int day;
        
        @Schema(description = "일기 목록 (최대 3개)", requiredMode = Schema.RequiredMode.REQUIRED)
        private List<DiaryDetailDTO> diaries;
    }
    
    /**
     * 날짜별 미리보기 응답
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "날짜별 일기 미리보기 응답")
    public static class DailyPreviewResponseDTO {
        
        @Schema(description = "년도", example = "2025", requiredMode = Schema.RequiredMode.REQUIRED)
        private int year;
        
        @Schema(description = "월", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private int month;
        
        @Schema(description = "일", example = "15", requiredMode = Schema.RequiredMode.REQUIRED)
        private int day;
        
        @Schema(description = "일기 미리보기 목록 (최대 3개)", requiredMode = Schema.RequiredMode.REQUIRED)
        private List<DiaryPreviewDTO> diaries;
    }

    /**
     * 커서 기반 페이지 정보
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "피드 페이지 정보")
    public static class FeedPageInfoDTO {

        @Schema(description = "다음 페이지 존재 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        private boolean hasNext;

        @Schema(description = "다음 커서 (없으면 null)", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private FeedNextCursorDTO nextCursor;
    }

    /**
     * 다음 커서
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "피드 다음 커서")
    public static class FeedNextCursorDTO {

        @Schema(description = "커서 createdAt (ISO-8601, 서버가 내려준 값을 그대로 재전송)", example = "2025-12-30T21:15:10.123456", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDateTime cursorCreatedAt;

        @Schema(description = "커서 diaryId", example = "401", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long cursorDiaryId;
    }

    /**
     * 응답 (월간 전체조회(DailySummaryResponseDTO) 구조 재사용)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "피드 조회 응답")
    public static class FeedResponseDTO {

        @Schema(description = "일자별 일기 목록 (최신순 피드)", requiredMode = Schema.RequiredMode.REQUIRED)
        private List<DailySummaryResponseDTO> days;

        @Schema(description = "페이지 정보", requiredMode = Schema.RequiredMode.REQUIRED)
        private FeedPageInfoDTO pageInfo;
    }
}
