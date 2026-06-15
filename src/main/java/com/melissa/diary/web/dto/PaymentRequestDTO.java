package com.melissa.diary.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

public class PaymentRequestDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Google Play one-time product verify request")
    public static class GoogleVerifyRequest {

        @NotBlank
        @Schema(description = "Google Play product id", example = "premium", requiredMode = Schema.RequiredMode.REQUIRED)
        private String productId;

        @NotBlank
        @Schema(description = "Android package name", example = "com.melissa.melissaFE", requiredMode = Schema.RequiredMode.REQUIRED)
        private String packageName;

        @NotBlank
        @Schema(description = "Google Play Billing purchase token", requiredMode = Schema.RequiredMode.REQUIRED)
        private String purchaseToken;

        @Schema(description = "Google order id", example = "GPA.1234-5678-9012-34567", nullable = true)
        private String orderId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Google Play restore request")
    public static class GoogleRestoreRequest {

        @Valid
        @NotEmpty
        private List<GoogleVerifyRequest> purchases;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "Apple App Store transaction verify request")
    public static class AppleVerifyRequest {

        @NotBlank
        @Schema(description = "Apple product id", example = "com.melissa.melissaFE.premium", requiredMode = Schema.RequiredMode.REQUIRED)
        private String productId;

        @NotBlank
        @Schema(description = "Apple transaction id", example = "2000000123456789", requiredMode = Schema.RequiredMode.REQUIRED)
        private String transactionId;

        @Schema(description = "Apple original transaction id", example = "2000000123456789", nullable = true)
        private String originalTransactionId;

        @NotBlank
        @Schema(description = "Apple transaction environment", example = "SANDBOX", requiredMode = Schema.RequiredMode.REQUIRED)
        private String environment;

        @NotBlank
        @Schema(description = "iOS bundle id", example = "com.melissa.melissaFE", requiredMode = Schema.RequiredMode.REQUIRED)
        private String bundleId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Apple App Store restore request")
    public static class AppleRestoreRequest {

        @Valid
        @NotEmpty
        private List<AppleVerifyRequest> transactions;
    }
}
