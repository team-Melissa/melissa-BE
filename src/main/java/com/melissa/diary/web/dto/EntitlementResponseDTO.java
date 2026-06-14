package com.melissa.diary.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class EntitlementResponseDTO {

    @Getter
    @Builder
    @Schema(description = "Current user's active entitlement response")
    public static class EntitlementsResponse {

        @Schema(description = "Active entitlement list")
        private List<EntitlementSummary> entitlements;

        @Schema(description = "Feature flags derived from active entitlements")
        private FeatureSummary features;
    }

    @Getter
    @Builder
    @Schema(description = "Active entitlement summary")
    public static class EntitlementSummary {

        @Schema(description = "Entitlement type", example = "REMOVE_ADS")
        private String type;

        @Schema(description = "Whether the entitlement is active", example = "true")
        private Boolean active;

        @Schema(description = "Source platform", example = "GOOGLE", nullable = true)
        private String sourcePlatform;

        @Schema(description = "Granted time")
        private LocalDateTime grantedAt;

        @Schema(description = "Revoked time", nullable = true)
        private LocalDateTime revokedAt;
    }

    @Getter
    @Builder
    @Schema(description = "Feature flags")
    public static class FeatureSummary {

        @Schema(description = "Whether ads should be removed", example = "true")
        private Boolean adRemoved;
    }
}
