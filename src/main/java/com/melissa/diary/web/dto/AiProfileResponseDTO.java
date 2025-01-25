package com.melissa.diary.web.dto;

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

        // 질문 원문
        private String feature1;  // Q1
        private String feature2;  // Q2
        private String feature3;  // Q3
        private String feature4;  // Q4
        private String feature5;  // Q5
        private String feature6;  // Q6

        // 해시태그
        private String hashTag1;  // 성격
        private String hashTag2;  // 연령대
        private String hashTag3;  // 목적성


        private LocalDateTime createdAt;

        }
}
