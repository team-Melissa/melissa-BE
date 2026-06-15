package com.melissa.diary.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class PaymentResponseDTO {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Store purchase verify response")
    public static class VerifyResponse {
        private Long paymentId;
        private String platform;
        private String productId;
        private String status;
        private EntitlementSummary entitlement;
        private Boolean acknowledged;
        private Boolean finishRequired;
        private Boolean alreadyProcessed;
        private LocalDateTime purchasedAt;
        private LocalDateTime verifiedAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Payment entitlement summary")
    public static class EntitlementSummary {
        private String type;
        private Boolean active;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Store restore response")
    public static class RestoreResponse {
        private List<RestoredPurchase> restored;
        private List<FailedPurchase> failed;
        private EntitlementResponseDTO.FeatureSummary features;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Restored purchase summary")
    public static class RestoredPurchase {
        private Long paymentId;
        private String platform;
        private String productId;
        private String status;
        private String entitlementType;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Failed restore item summary")
    public static class FailedPurchase {
        private String platform;
        private String productId;
        private String identifier;
        private String code;
        private String message;
    }
}
