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

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "관리자 수동 환불 반영 응답")
    public static class AdminManualRefundResponse {
        @Schema(description = "백엔드 결제 내역 ID", example = "1")
        private Long paymentId;

        @Schema(description = "환불 처리 대상 사용자 ID", example = "10")
        private Long userId;

        @Schema(description = "결제 플랫폼", example = "GOOGLE")
        private String platform;

        @Schema(description = "백엔드 내부 상품 ID", example = "remove_ads")
        private String productId;

        @Schema(description = "결제 상태", example = "REFUNDED")
        private String status;

        @Schema(description = "회수 대상 권한 타입", example = "REMOVE_ADS", nullable = true)
        private String entitlementType;

        @Schema(description = "권한 회수 완료 여부", example = "true")
        private Boolean entitlementRevoked;

        @Schema(description = "권한 활성 여부", example = "false", nullable = true)
        private Boolean entitlementActive;

        @Schema(description = "이미 환불 처리된 결제였는지 여부", example = "false")
        private Boolean alreadyProcessed;

        @Schema(description = "환불 반영 시각")
        private LocalDateTime refundedAt;

        @Schema(description = "관리자 fallback 처리 시각")
        private LocalDateTime processedAt;
    }
}
