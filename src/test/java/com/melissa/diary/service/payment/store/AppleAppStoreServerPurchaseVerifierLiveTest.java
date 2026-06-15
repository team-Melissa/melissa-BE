package com.melissa.diary.service.payment.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melissa.diary.config.PaymentProperties;
import com.melissa.diary.domain.enums.PaymentPlatform;
import com.melissa.diary.web.dto.PaymentRequestDTO;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class AppleAppStoreServerPurchaseVerifierLiveTest {

    @Test
    void verifyAppleTransactionAgainstAppStoreServerApiWhenEnvIsPresent() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        PaymentRequestDTO.AppleVerifyRequest request = appleVerifyRequest(objectMapper);
        String issuerId = env("APPLE_ISSUER_ID");
        String keyId = env("APPLE_KEY_ID");
        String privateKeyP8Base64 = privateKeyP8Base64();

        Assumptions.assumeTrue(!isBlank(request.getTransactionId()), "Apple transactionId is not set");
        Assumptions.assumeTrue(!isBlank(issuerId), "APPLE_ISSUER_ID is not set");
        Assumptions.assumeTrue(!isBlank(keyId), "APPLE_KEY_ID is not set");
        Assumptions.assumeTrue(!isBlank(privateKeyP8Base64), "Apple private key env is not set");

        String bundleId = request.getBundleId();
        String productId = request.getProductId();
        String environment = request.getEnvironment();

        PaymentProperties properties = new PaymentProperties();
        properties.getIap().setVerifyTimeoutMs(15000);
        properties.getApple().setIssuerId(issuerId);
        properties.getApple().setKeyId(keyId);
        properties.getApple().setPrivateKeyP8Base64(privateKeyP8Base64);
        properties.getApple().setBundleId(bundleId);
        properties.getApple().setProductRemoveAds(productId);
        properties.getApple().setEnvironment(environment);

        AppleAppStoreServerPurchaseVerifier verifier = new AppleAppStoreServerPurchaseVerifier(properties, objectMapper);
        VerifiedPurchase purchase = verifier.verify(new ApplePurchaseVerifyCommand(
                productId,
                request.getTransactionId(),
                request.getOriginalTransactionId(),
                environment,
                bundleId
        ));

        assertThat(purchase.getPlatform()).isEqualTo(PaymentPlatform.APPLE);
        assertThat(purchase.getAppleTransactionId()).isEqualTo(request.getTransactionId());
        assertThat(purchase.getStoreProductId()).isEqualTo(productId);
    }

    private PaymentRequestDTO.AppleVerifyRequest appleVerifyRequest(ObjectMapper objectMapper) throws Exception {
        String payloadJson = payloadJson();
        if (!isBlank(payloadJson)) {
            return objectMapper.readValue(payloadJson, PaymentRequestDTO.AppleVerifyRequest.class);
        }

        return new PaymentRequestDTO.AppleVerifyRequest(
                envOrDefault("PAYMENT_TEST_APPLE_PRODUCT_ID", "com.melissa.melissaFE.premium"),
                env("PAYMENT_TEST_APPLE_TRANSACTION_ID"),
                env("PAYMENT_TEST_APPLE_ORIGINAL_TRANSACTION_ID"),
                envOrDefault("PAYMENT_TEST_APPLE_ENVIRONMENT", "SANDBOX"),
                envOrDefault("PAYMENT_TEST_APPLE_BUNDLE_ID", "com.melissa.melissaFE")
        );
    }

    private String payloadJson() throws Exception {
        String json = env("PAYMENT_TEST_APPLE_VERIFY_REQUEST_JSON");
        if (!isBlank(json)) {
            return json;
        }

        String path = env("PAYMENT_TEST_APPLE_VERIFY_REQUEST_JSON_PATH");
        if (isBlank(path)) {
            return "";
        }

        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }

    private String privateKeyP8Base64() throws Exception {
        String encoded = env("APPLE_PRIVATE_KEY_P8_BASE64");
        if (!isBlank(encoded)) {
            return encoded;
        }

        String path = env("APPLE_PRIVATE_KEY_P8_PATH");
        if (isBlank(path)) {
            return "";
        }

        String pem = Files.readString(Path.of(path), StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(pem.getBytes(StandardCharsets.UTF_8));
    }

    private String envOrDefault(String name, String defaultValue) {
        String value = env(name);
        return isBlank(value) ? defaultValue : value;
    }

    private String env(String name) {
        return System.getenv(name);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
