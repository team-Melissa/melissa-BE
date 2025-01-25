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
}
