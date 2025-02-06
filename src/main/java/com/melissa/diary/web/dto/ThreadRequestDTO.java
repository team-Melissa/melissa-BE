package com.melissa.diary.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ThreadRequestDTO {
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiChatRequest {
        private String content;
        private int year;
        private int month;
        private int day;
    }
}
