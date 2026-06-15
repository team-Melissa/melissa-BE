package com.melissa.diary.service.payment.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.config.PaymentProperties;
import com.melissa.diary.domain.enums.PaymentPlatform;
import com.melissa.diary.domain.enums.ProductType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GooglePlayRestPurchaseVerifier implements GooglePlayPurchaseVerifier {

    private static final String ANDROID_PUBLISHER_SCOPE = "https://www.googleapis.com/auth/androidpublisher";
    private static final String GOOGLE_ANDROID_PUBLISHER_BASE_URL = "https://androidpublisher.googleapis.com";

    private final PaymentProperties paymentProperties;
    private final ObjectMapper objectMapper;

    @Override
    public VerifiedPurchase verify(GooglePurchaseVerifyCommand command) {
        try {
            HttpResponse<String> response = sendGetPurchaseRequest(command);
            if (!isSuccess(response.statusCode())) {
                log.warn("[GooglePlayRestPurchaseVerifier] getProductPurchase failed. statusCode={}, packageName={}, productId={}",
                        response.statusCode(), command.packageName(), command.productId());
                throw statusError(response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            assertPurchased(root);

            String responseProductId = readProductId(root);
            if (responseProductId != null && !command.productId().equals(responseProductId)) {
                throw new ErrorHandler(ErrorStatus.PAYMENT_PRODUCT_MISMATCH);
            }

            return VerifiedPurchase.builder()
                    .platform(PaymentPlatform.GOOGLE)
                    .storeProductId(command.productId())
                    .productType(ProductType.NON_CONSUMABLE)
                    .orderId(firstText(root, "orderId", command.orderId()))
                    .purchasedAt(readPurchaseTime(root))
                    .acknowledged(isAcknowledged(root))
                    .rawPayload(objectMapper.writeValueAsString(root))
                    .build();
        } catch (ErrorHandler e) {
            throw e;
        } catch (Exception e) {
            log.warn("[GooglePlayRestPurchaseVerifier] verify failed. packageName={}, productId={}",
                    command.packageName(), command.productId(), e);
            throw new ErrorHandler(ErrorStatus.PAYMENT_STORE_API_FAILED);
        }
    }

    @Override
    public void acknowledge(GoogleAcknowledgeCommand command) {
        try {
            HttpResponse<String> response = sendAcknowledgeRequest(command);
            if (!isSuccess(response.statusCode())) {
                log.warn("[GooglePlayRestPurchaseVerifier] acknowledge failed. statusCode={}, packageName={}, productId={}",
                        response.statusCode(), command.packageName(), command.productId());
                throw statusError(response.statusCode());
            }
        } catch (ErrorHandler e) {
            throw e;
        } catch (Exception e) {
            log.warn("[GooglePlayRestPurchaseVerifier] acknowledge failed. packageName={}, productId={}",
                    command.packageName(), command.productId(), e);
            throw new ErrorHandler(ErrorStatus.PAYMENT_ACKNOWLEDGE_FAILED);
        }
    }

    private HttpResponse<String> sendGetPurchaseRequest(GooglePurchaseVerifyCommand command) throws Exception {
        String url = GOOGLE_ANDROID_PUBLISHER_BASE_URL
                + "/androidpublisher/v3/applications/" + encodePath(command.packageName())
                + "/purchases/productsv2/tokens/" + encodePath(command.purchaseToken());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(paymentProperties.getIap().getVerifyTimeoutMs()))
                .header("Authorization", "Bearer " + accessToken())
                .GET()
                .build();

        return httpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendAcknowledgeRequest(GoogleAcknowledgeCommand command) throws Exception {
        String url = GOOGLE_ANDROID_PUBLISHER_BASE_URL
                + "/androidpublisher/v3/applications/" + encodePath(command.packageName())
                + "/purchases/products/" + encodePath(command.productId())
                + "/tokens/" + encodePath(command.purchaseToken())
                + ":acknowledge";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(paymentProperties.getIap().getVerifyTimeoutMs()))
                .header("Authorization", "Bearer " + accessToken())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        return httpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String accessToken() throws Exception {
        String encodedJson = paymentProperties.getGoogle().getServiceAccountJsonBase64();
        if (isBlank(encodedJson)) {
            throw new ErrorHandler(ErrorStatus.PAYMENT_STORE_API_FAILED);
        }

        byte[] json = Base64.getDecoder().decode(encodedJson);
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new ByteArrayInputStream(json))
                .createScoped(List.of(ANDROID_PUBLISHER_SCOPE));
        credentials.refreshIfExpired();
        return credentials.getAccessToken().getTokenValue();
    }

    private void assertPurchased(JsonNode root) {
        String purchaseState = root.path("purchaseStateContext").path("purchaseState").asText("");
        if (!isBlank(purchaseState)
                && !"PURCHASED".equalsIgnoreCase(purchaseState)
                && !"PURCHASE_STATE_PURCHASED".equalsIgnoreCase(purchaseState)) {
            throw new ErrorHandler(ErrorStatus.PAYMENT_PURCHASE_NOT_COMPLETED);
        }
    }

    private String readProductId(JsonNode root) {
        JsonNode lineItems = root.path("productLineItem");
        if (lineItems.isArray() && !lineItems.isEmpty()) {
            return lineItems.get(0).path("productId").asText(null);
        }
        return null;
    }

    private boolean isAcknowledged(JsonNode root) {
        String acknowledgementState = root.path("acknowledgementState").asText("");
        return "ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED".equalsIgnoreCase(acknowledgementState)
                || "ACKNOWLEDGED".equalsIgnoreCase(acknowledgementState);
    }

    private LocalDateTime readPurchaseTime(JsonNode root) {
        String completionTime = root.path("purchaseCompletionTime").asText(null);
        if (!isBlank(completionTime)) {
            return LocalDateTime.ofInstant(Instant.parse(completionTime), ZoneOffset.UTC);
        }

        String purchaseTimeMillis = root.path("purchaseTimeMillis").asText(null);
        if (!isBlank(purchaseTimeMillis)) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(purchaseTimeMillis)), ZoneOffset.UTC);
        }

        return null;
    }

    private String firstText(JsonNode root, String fieldName, String fallback) {
        String value = root.path(fieldName).asText(null);
        return isBlank(value) ? fallback : value;
    }

    private RuntimeException statusError(int statusCode) {
        if (statusCode >= 500 || statusCode == 401 || statusCode == 403) {
            return new ErrorHandler(ErrorStatus.PAYMENT_STORE_API_FAILED);
        }
        return new ErrorHandler(ErrorStatus.PAYMENT_STORE_VERIFICATION_FAILED);
    }

    private HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(paymentProperties.getIap().getVerifyTimeoutMs()))
                .build();
    }

    private boolean isSuccess(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    private String encodePath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
