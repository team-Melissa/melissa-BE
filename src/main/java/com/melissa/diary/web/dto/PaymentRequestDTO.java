package com.melissa.diary.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class PaymentRequestDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Google Play 비소모성 상품 구매 검증 요청")
    public static class GoogleVerifyRequest {

        @NotBlank
        @Schema(description = "Google Play 상품 ID. 현재 후보: premium(광고 제거)", example = "premium", allowableValues = {"premium"}, requiredMode = Schema.RequiredMode.REQUIRED)
        private String productId;

        @NotBlank
        @Schema(description = "Android 앱 패키지명", example = "com.melissa.melissaFE", requiredMode = Schema.RequiredMode.REQUIRED)
        private String packageName;

        @NotBlank
        @Schema(description = "Google Play Billing purchaseToken", requiredMode = Schema.RequiredMode.REQUIRED)
        private String purchaseToken;

        @Schema(description = "Google 주문 ID. 없으면 생략 가능", example = "GPA.1234-5678-9012-34567", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String orderId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Google Play 구매 복원 요청")
    public static class GoogleRestoreRequest {

        @Valid
        @NotEmpty
        @Schema(description = "Google Play Billing에서 조회한 보유 구매 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        private List<GoogleVerifyRequest> purchases;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "Apple App Store 거래 검증 요청")
    public static class AppleVerifyRequest {

        @NotBlank
        @Schema(description = "Apple 인앱결제 상품 ID. 현재 후보: com.melissa.melissaFE.premium(광고 제거)", example = "com.melissa.melissaFE.premium", allowableValues = {"com.melissa.melissaFE.premium"}, requiredMode = Schema.RequiredMode.REQUIRED)
        private String productId;

        @NotBlank
        @Schema(description = "Apple transactionId", example = "2000000123456789", requiredMode = Schema.RequiredMode.REQUIRED)
        private String transactionId;

        @Schema(description = "Apple originalTransactionId. 없으면 생략 가능", example = "2000000123456789", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String originalTransactionId;

        @NotBlank
        @Schema(description = "Apple 거래 환경. 후보: SANDBOX(테스트 결제), PRODUCTION(상용 결제)", example = "SANDBOX", allowableValues = {"SANDBOX", "PRODUCTION"}, requiredMode = Schema.RequiredMode.REQUIRED)
        private String environment;

        @NotBlank
        @Schema(description = "iOS 앱 bundleId", example = "com.melissa.melissaFE", requiredMode = Schema.RequiredMode.REQUIRED)
        private String bundleId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Apple App Store 구매 복원 요청")
    public static class AppleRestoreRequest {

        @Valid
        @NotEmpty
        @Schema(description = "App Store에서 조회한 보유 거래 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        private List<AppleVerifyRequest> transactions;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "관리자 수동 환불 반영 요청")
    public static class AdminManualRefundRequest {

        @NotBlank
        @Size(max = 100)
        @Schema(description = "수동 환불 반영 사유", example = "Google Play Console 환불 처리 확인", requiredMode = Schema.RequiredMode.REQUIRED)
        private String reason;

        @Schema(description = "스토어 환불 처리 시각. 생략하면 서버 처리 시각 사용", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private LocalDateTime refundedAt;
    }
}
