package com.melissa.diary.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class EntitlementResponseDTO {

    @Getter
    @Builder
    @Schema(description = "현재 사용자의 활성 권한 응답")
    public static class EntitlementsResponse {

        @Schema(description = "활성 권한 목록")
        private List<EntitlementSummary> entitlements;

        @Schema(description = "활성 권한 기반 프론트 기능 플래그")
        private FeatureSummary features;
    }

    @Getter
    @Builder
    @Schema(description = "활성 권한 요약")
    public static class EntitlementSummary {

        @Schema(description = "권한 타입", example = "REMOVE_ADS")
        private String type;

        @Schema(description = "권한 활성 여부", example = "true")
        private Boolean active;

        @Schema(description = "권한이 부여된 결제 플랫폼", example = "GOOGLE", nullable = true)
        private String sourcePlatform;

        @Schema(description = "권한 부여 시각")
        private LocalDateTime grantedAt;

        @Schema(description = "권한 회수 시각", nullable = true)
        private LocalDateTime revokedAt;
    }

    @Getter
    @Builder
    @Schema(description = "프론트 기능 플래그")
    public static class FeatureSummary {

        @Schema(description = "광고 제거 적용 여부. 프론트는 이 값을 기준으로 광고 표시 여부를 판단", example = "true")
        private Boolean adRemoved;
    }
}
