package com.melissa.diary.service.payment.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melissa.diary.config.PaymentProperties;
import com.melissa.diary.domain.enums.PaymentPlatform;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class GooglePlayRestPurchaseVerifierLiveTest {

    @Test
    void verifyGooglePurchaseAgainstPlayDeveloperApiWhenEnvIsPresent() throws Exception {
        String purchaseToken = env("PAYMENT_TEST_GOOGLE_PURCHASE_TOKEN");
        String serviceAccountJsonBase64 = serviceAccountJsonBase64();

        Assumptions.assumeTrue(!isBlank(purchaseToken), "PAYMENT_TEST_GOOGLE_PURCHASE_TOKEN is not set");
        Assumptions.assumeTrue(!isBlank(serviceAccountJsonBase64), "Google service account env is not set");

        String packageName = envOrDefault("PAYMENT_TEST_GOOGLE_PACKAGE_NAME", "com.melissa.melissaFE");
        String productId = envOrDefault("PAYMENT_TEST_GOOGLE_PRODUCT_ID", "premium");

        PaymentProperties properties = new PaymentProperties();
        properties.getIap().setVerifyTimeoutMs(15000);
        properties.getGoogle().setServiceAccountJsonBase64(serviceAccountJsonBase64);

        GooglePlayRestPurchaseVerifier verifier = new GooglePlayRestPurchaseVerifier(properties, new ObjectMapper());
        VerifiedPurchase purchase = verifier.verify(new GooglePurchaseVerifyCommand(
                packageName,
                productId,
                purchaseToken,
                env("PAYMENT_TEST_GOOGLE_ORDER_ID")
        ));

        assertThat(purchase.getPlatform()).isEqualTo(PaymentPlatform.GOOGLE);
        assertThat(purchase.getStoreProductId()).isEqualTo(productId);
    }

    private String serviceAccountJsonBase64() throws Exception {
        String encoded = env("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_BASE64");
        if (!isBlank(encoded)) {
            return encoded;
        }

        String path = env("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_PATH");
        if (isBlank(path)) {
            return "";
        }
        String json = Files.readString(Path.of(path), StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
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
