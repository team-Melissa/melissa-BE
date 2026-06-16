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
    @Schema(description = "스토어 구매 검증 응답")
    public static class VerifyResponse {
        @Schema(description = "백엔드 결제 내역 ID", example = "1")
        private Long paymentId;

        @Schema(description = "결제 플랫폼. 후보: GOOGLE, APPLE", example = "GOOGLE", allowableValues = {"GOOGLE", "APPLE"})
        private String platform;

        @Schema(description = "백엔드 내부 상품 ID. 현재 후보: remove_ads(광고 제거). 스토어 상품 ID가 아니라 내부 상품 ID로 반환", example = "remove_ads", allowableValues = {"remove_ads"})
        private String productId;

        @Schema(description = "결제 상태. 후보: PENDING, PURCHASED, FAILED, REFUNDED, REVOKED", example = "PURCHASED", allowableValues = {"PENDING", "PURCHASED", "FAILED", "REFUNDED", "REVOKED"})
        private String status;

        @Schema(description = "검증 성공으로 부여된 권한 요약")
        private EntitlementSummary entitlement;

        @Schema(description = "Google 구매 acknowledge 완료 여부. Apple은 null", example = "true", nullable = true)
        private Boolean acknowledged;

        @Schema(description = "프론트의 구매 완료 처리 필요 여부. Apple 검증 성공 시 true", example = "true", nullable = true)
        private Boolean finishRequired;

        @Schema(description = "이미 처리된 같은 구매를 다시 검증했는지 여부", example = "false")
        private Boolean alreadyProcessed;

        @Schema(description = "스토어 구매 시각")
        private LocalDateTime purchasedAt;

        @Schema(description = "백엔드 검증 시각")
        private LocalDateTime verifiedAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "결제 권한 요약")
    public static class EntitlementSummary {
        @Schema(description = "권한 타입. enum 후보: REMOVE_ADS(광고 제거), PREMIUM, EXTRA_STORAGE, AI_CREDIT. 현재 결제 상품은 REMOVE_ADS만 사용", example = "REMOVE_ADS", allowableValues = {"REMOVE_ADS", "PREMIUM", "EXTRA_STORAGE", "AI_CREDIT"})
        private String type;

        @Schema(description = "권한 활성 여부. true면 광고 제거 적용 가능", example = "true")
        private Boolean active;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "스토어 구매 복원 응답")
    public static class RestoreResponse {
        @Schema(description = "복원에 성공한 구매 목록")
        private List<RestoredPurchase> restored;

        @Schema(description = "검증에 실패한 구매 목록. 일부 실패가 있어도 API 자체는 성공할 수 있음")
        private List<FailedPurchase> failed;

        @Schema(description = "현재 사용자의 기능 플래그. 광고 제거 여부는 `adRemoved`로 판단")
        private EntitlementResponseDTO.FeatureSummary features;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "복원 성공 구매 요약")
    public static class RestoredPurchase {
        @Schema(description = "백엔드 결제 내역 ID", example = "1")
        private Long paymentId;

        @Schema(description = "결제 플랫폼. 후보: GOOGLE, APPLE", example = "APPLE", allowableValues = {"GOOGLE", "APPLE"})
        private String platform;

        @Schema(description = "백엔드 내부 상품 ID. 현재 후보: remove_ads(광고 제거)", example = "remove_ads", allowableValues = {"remove_ads"})
        private String productId;

        @Schema(description = "결제 상태. 후보: PENDING, PURCHASED, FAILED, REFUNDED, REVOKED", example = "PURCHASED", allowableValues = {"PENDING", "PURCHASED", "FAILED", "REFUNDED", "REVOKED"})
        private String status;

        @Schema(description = "복원된 권한 타입. enum 후보: REMOVE_ADS(광고 제거), PREMIUM, EXTRA_STORAGE, AI_CREDIT. 현재 결제 상품은 REMOVE_ADS만 사용", example = "REMOVE_ADS", allowableValues = {"REMOVE_ADS", "PREMIUM", "EXTRA_STORAGE", "AI_CREDIT"})
        private String entitlementType;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "복원 실패 구매 요약")
    public static class FailedPurchase {
        @Schema(description = "결제 플랫폼. 후보: GOOGLE, APPLE", example = "GOOGLE", allowableValues = {"GOOGLE", "APPLE"})
        private String platform;

        @Schema(description = "요청으로 전달된 스토어 상품 ID", example = "premium")
        private String productId;

        @Schema(description = "실패 항목 식별자. Google은 orderId 또는 purchaseToken, Apple은 transactionId", example = "GPA.1234-5678-9012-34567")
        private String identifier;

        @Schema(description = "실패 코드", example = "PAYMENT4004")
        private String code;

        @Schema(description = "실패 메시지", example = "스토어 구매 검증에 실패했습니다.")
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

        @Schema(description = "결제 플랫폼. 후보: GOOGLE, APPLE", example = "GOOGLE", allowableValues = {"GOOGLE", "APPLE"})
        private String platform;

        @Schema(description = "백엔드 내부 상품 ID. 현재 후보: remove_ads(광고 제거)", example = "remove_ads", allowableValues = {"remove_ads"})
        private String productId;

        @Schema(description = "결제 상태. 후보: PENDING, PURCHASED, FAILED, REFUNDED, REVOKED", example = "REFUNDED", allowableValues = {"PENDING", "PURCHASED", "FAILED", "REFUNDED", "REVOKED"})
        private String status;

        @Schema(description = "회수 대상 권한 타입. enum 후보: REMOVE_ADS(광고 제거), PREMIUM, EXTRA_STORAGE, AI_CREDIT. 회수 대상 권한이 없으면 null", example = "REMOVE_ADS", allowableValues = {"REMOVE_ADS", "PREMIUM", "EXTRA_STORAGE", "AI_CREDIT"}, nullable = true)
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
