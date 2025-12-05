package com.melissa.diary.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "일기 삭제 응답")
    public static class DiaryDeleteResponse {
        
        @Schema(description = "삭제된 일기 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long diaryId;
        
        @Schema(description = "결과 메시지", example = "일기가 삭제되었습니다.", requiredMode = Schema.RequiredMode.REQUIRED)
        private String message;
    }
    
    /**
     * 일기 생성/수정 응답
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "일기 생성/수정 응답")
    public static class DiaryResponse {
        
        @Schema(description = "일기 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long diaryId;
        
        @Schema(description = "스레드 ID (항상 존재)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long threadId;
        
        @Schema(description = "년도", example = "2025", requiredMode = Schema.RequiredMode.REQUIRED)
        private int year;
        
        @Schema(description = "월", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private int month;
        
        @Schema(description = "일", example = "15", requiredMode = Schema.RequiredMode.REQUIRED)
        private int day;
        
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
        
        @Schema(description = "이미지 URL (비동기 생성, 초기에는 null)", example = "https://s3.amazonaws.com/...", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String imageUrl;
        
        @Schema(description = "버전", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private int version;
        
        @Schema(description = "생성일시", example = "2025-01-01T12:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDateTime createdAt;
    }
}

