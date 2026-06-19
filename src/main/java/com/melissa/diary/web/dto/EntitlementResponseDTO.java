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

        @Schema(description = "활성 권한 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        private List<EntitlementSummary> entitlements;

        @Schema(description = "활성 권한 기반 프론트 기능 플래그", requiredMode = Schema.RequiredMode.REQUIRED)
        private FeatureSummary features;
    }

    @Getter
    @Builder
    @Schema(description = "활성 권한 요약")
    public static class EntitlementSummary {

        @Schema(
                description = "권한 타입. enum 후보: REMOVE_ADS(광고 제거), PREMIUM, EXTRA_STORAGE, AI_CREDIT. 현재 결제 상품은 REMOVE_ADS만 사용",
                example = "REMOVE_ADS",
                allowableValues = {"REMOVE_ADS", "PREMIUM", "EXTRA_STORAGE", "AI_CREDIT"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String type;

        @Schema(description = "권한 활성 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        private Boolean active;

        @Schema(
                description = "권한이 부여된 결제 플랫폼. 후보: GOOGLE, APPLE. 결제 외 수동 부여 등 플랫폼이 없으면 null",
                example = "GOOGLE",
                allowableValues = {"GOOGLE", "APPLE"},
                requiredMode = Schema.RequiredMode.REQUIRED,
                nullable = true
        )
        private String sourcePlatform;

        @Schema(description = "권한 부여 시각", requiredMode = Schema.RequiredMode.REQUIRED)
        private LocalDateTime grantedAt;

        @Schema(description = "권한 회수 시각", requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        private LocalDateTime revokedAt;
    }

    @Getter
    @Builder
    @Schema(description = "프론트 기능 플래그")
    public static class FeatureSummary {

        @Schema(description = "광고 제거 적용 여부. 프론트는 이 값을 기준으로 광고 표시 여부를 판단", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        private Boolean adRemoved;
    }
}
