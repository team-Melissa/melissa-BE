package com.melissa.diary.web.dto;

import jakarta.persistence.Column;
import lombok.*;

import java.time.LocalDateTime;

public class AiProfileResponseDTO {
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiProfileResponse {

        private Long aiProfileId;

        private String profileName;

        private String imageUrl;

        // 아래 정보는 질문 바탕으로 프롬프팅
        // 해시태그 최대 2개
        private String hashTag1;
        private String hashTag2;

        // 특징(Feature) 최대 3개
        private String feature1;
        private String feature2;
        private String feature3;


        private LocalDateTime createdAt;

        }
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiProfileQuestionResponse {

        private String q1;

        private String q2;

        private String q3;

        private String q4;

        private String q5;

        private String q6;

        private LocalDateTime createdAt;

    }
}
