package com.melissa.diary.web.dto;

import lombok.*;

public class AiProfileRequestDTO {
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiProfileCreateRequest {

        private String q1;

        private String q2;

        private String q3;

        private String q4;

        private String q5;

        private String q6;
    }
}
