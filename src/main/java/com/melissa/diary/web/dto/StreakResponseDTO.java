package com.melissa.diary.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class StreakResponseDTO {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "스트릭 조회 응답")
    public static class CurrentStreakResponse {

        @Schema(description = "현재 스트릭(연속 작성 일수). 오늘 작성이 없으면 0", example = "7", requiredMode = Schema.RequiredMode.REQUIRED)
        private int streakDays;
    }
}


