package com.melissa.diary.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

public class AiProfileResponseDTO {
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "AI 프로필 응답")
    public static class AiProfileResponse {

        @Schema(description = "AI 프로필 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long aiProfileId;

        @Schema(description = "프로필 이름", example = "루나", requiredMode = Schema.RequiredMode.REQUIRED)
        private String profileName;

        @Schema(description = "프로필 이미지 URL (서버 주입)", example = "https://s3.amazonaws.com/...", requiredMode = Schema.RequiredMode.REQUIRED)
        private String imageUrl;

        // 아래 정보는 질문 바탕으로 프롬프팅
        // 해시태그 최대 2개
        @Schema(description = "해시태그 1 (서버 주입)", example = "#위로", requiredMode = Schema.RequiredMode.REQUIRED)
        private String hashTag1;
        
        @Schema(description = "해시태그 2 (서버 주입)", example = "#공감", requiredMode = Schema.RequiredMode.REQUIRED)
        private String hashTag2;

        // 특징(Feature) 최대 3개
        @Schema(description = "특징 1 (서버 주입)", example = "공감을 잘해요", requiredMode = Schema.RequiredMode.REQUIRED)
        private String feature1;
        
        @Schema(description = "특징 2 (서버 주입)", example = "위로를 잘해요", requiredMode = Schema.RequiredMode.REQUIRED)
        private String feature2;
        
        @Schema(description = "특징 3 (서버 주입)", example = "따뜻해요", requiredMode = Schema.RequiredMode.REQUIRED)
        private String feature3;

        @Schema(description = "생성일시", example = "2025-01-01T12:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDateTime createdAt;

        @Schema(description = "기본 프로필 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        private boolean isDefault;
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "AI 프로필 생성 질문 응답")
    public static class AiProfileQuestionResponse {

        @Schema(description = "질문 1 (서버 주입)", example = "어떤 말투를 원하시나요?", requiredMode = Schema.RequiredMode.REQUIRED)
        private String q1;

        @Schema(description = "질문 2 (서버 주입)", example = "답변 길이는 어떻게 할까요?", requiredMode = Schema.RequiredMode.REQUIRED)
        private String q2;

        @Schema(description = "질문 3 (서버 주입)", example = "어떤 성격이면 좋겠나요?", requiredMode = Schema.RequiredMode.REQUIRED)
        private String q3;

        @Schema(description = "질문 4 (서버 주입)", example = "어떤 대화를 원하시나요?", requiredMode = Schema.RequiredMode.REQUIRED)
        private String q4;

        @Schema(description = "질문 5 (서버 주입)", example = "어떤 반응을 원하시나요?", requiredMode = Schema.RequiredMode.REQUIRED)
        private String q5;

        @Schema(description = "질문 6 (서버 주입)", example = "추가 요청사항이 있나요?", requiredMode = Schema.RequiredMode.REQUIRED)
        private String q6;

        @Schema(description = "생성일시", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDateTime createdAt;
    }
}
