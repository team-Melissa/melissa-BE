package com.melissa.diary.service.payment.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.config.PaymentProperties;
import com.melissa.diary.domain.enums.PaymentPlatform;
import com.melissa.diary.domain.enums.ProductType;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
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
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppleAppStoreServerPurchaseVerifier implements AppleStorePurchaseVerifier {

    private static final String APP_STORE_AUDIENCE = "appstoreconnect-v1";
    private static final String SANDBOX_BASE_URL = "https://api.storekit-sandbox.itunes.apple.com";
    private static final String PRODUCTION_BASE_URL = "https://api.storekit.itunes.apple.com";
    private static final Set<String> TRUSTED_APPLE_ROOT_SHA256_FINGERPRINTS = Set.of(
            "63343ABFB89A6A03EBB57E9B3F5FA7BE7C4F5C756F3017B3A8C488C3653E9179",
            "C2B9B042DD57830E7D117DAC55AC8AE19407D38E41D88F3215BC3A890444A050",
            "B0B1730ECBC7FF4505142C49F1295E6EDA6BCAED7E2C68C5BE91B5A11001F024"
    );

    private final PaymentProperties paymentProperties;
    private final ObjectMapper objectMapper;

    @Override
    public VerifiedPurchase verify(ApplePurchaseVerifyCommand command) {
        try {
            TransactionPayload payload = loadTransactionPayload(command);
            JsonNode transaction = payload.transaction();
            assertTransactionMatches(command, transaction);

            ObjectNode rawPayload = objectMapper.createObjectNode();
            rawPayload.put("source", payload.source());
            if (payload.serverResponse() != null) {
                rawPayload.set("serverResponse", payload.serverResponse());
            }
            rawPayload.set("transaction", transaction);

            return VerifiedPurchase.builder()
                    .platform(PaymentPlatform.APPLE)
                    .storeProductId(transaction.path("productId").asText())
                    .productType(ProductType.NON_CONSUMABLE)
                    .appleTransactionId(transaction.path("transactionId").asText())
                    .appleOriginalTransactionId(firstText(transaction, "originalTransactionId", command.originalTransactionId()))
                    .appleEnvironment(normalizeEnvironment(transaction.path("environment").asText()))
                    .purchasedAt(readMillisTime(transaction, "purchaseDate"))
                    .revokedAt(readMillisTime(transaction, "revocationDate"))
                    .amountMicros(readApplePriceAsMicros(transaction))
                    .currency(transaction.path("currency").asText(null))
                    .revoked(hasField(transaction, "revocationDate"))
                    .rawPayload(objectMapper.writeValueAsString(rawPayload))
                    .build();
        } catch (ErrorHandler e) {
            throw e;
        } catch (Exception e) {
            log.warn("[AppleAppStoreServerPurchaseVerifier] verify failed. transactionId={}, productId={}",
                    command.transactionId(), command.productId(), e);
            throw new ErrorHandler(ErrorStatus.PAYMENT_STORE_API_FAILED);
        }
    }

    private TransactionPayload loadTransactionPayload(ApplePurchaseVerifyCommand command) throws Exception {
        HttpResponse<String> response = sendGetTransactionInfoRequest(command.transactionId());
        if (!isSuccess(response.statusCode())) {
            log.warn("[AppleAppStoreServerPurchaseVerifier] getTransactionInfo failed. statusCode={}, transactionId={}",
                    response.statusCode(), command.transactionId());
            throw statusError(response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String signedTransactionInfo = root.path("signedTransactionInfo").asText(null);
        if (isBlank(signedTransactionInfo)) {
            throw new ErrorHandler(ErrorStatus.PAYMENT_STORE_VERIFICATION_FAILED);
        }

        return new TransactionPayload(
                "APP_STORE_SERVER_API",
                verifyAndDecodeSignedTransaction(signedTransactionInfo),
                root
        );
    }

    private HttpResponse<String> sendGetTransactionInfoRequest(String transactionId) throws Exception {
        String url = baseUrl()
                + "/inApps/v1/transactions/"
                + URLEncoder.encode(transactionId, StandardCharsets.UTF_8).replace("+", "%20");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(paymentProperties.getIap().getVerifyTimeoutMs()))
                .header("Authorization", "Bearer " + authorizationToken())
                .GET()
                .build();

        return httpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String authorizationToken() throws Exception {
        PaymentProperties.Apple apple = paymentProperties.getApple();
        if (isBlank(apple.getIssuerId()) || isBlank(apple.getKeyId()) || isBlank(apple.getPrivateKeyP8Base64())) {
            throw new ErrorHandler(ErrorStatus.PAYMENT_STORE_API_FAILED);
        }

        Instant now = Instant.now();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer(apple.getIssuerId())
                .audience(APP_STORE_AUDIENCE)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(20 * 60)))
                .claim("bid", apple.getBundleId())
                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                .keyID(apple.getKeyId())
                .type(JOSEObjectType.JWT)
                .build();
        SignedJWT jwt = new SignedJWT(header, claimsSet);
        jwt.sign(new ECDSASigner(privateKey()));
        return jwt.serialize();
    }

    private ECPrivateKey privateKey() throws Exception {
        String pem = new String(
                Base64.getDecoder().decode(paymentProperties.getApple().getPrivateKeyP8Base64()),
                StandardCharsets.UTF_8
        );
        String encoded = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(encoded);
        return (ECPrivateKey) KeyFactory.getInstance("EC")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    private JsonNode verifyAndDecodeSignedTransaction(String signedPayload) throws Exception {
        JWSObject jwsObject = JWSObject.parse(signedPayload);
        verifySignedTransaction(jwsObject);
        return objectMapper.readTree(jwsObject.getPayload().toString());
    }

    private void verifySignedTransaction(JWSObject jwsObject) throws Exception {
        if (!JWSAlgorithm.ES256.equals(jwsObject.getHeader().getAlgorithm())) {
            throw new ErrorHandler(ErrorStatus.PAYMENT_STORE_VERIFICATION_FAILED);
        }

        List<X509Certificate> certificates = readX5cCertificates(jwsObject.getHeader());
        if (certificates.size() < 2) {
            throw new ErrorHandler(ErrorStatus.PAYMENT_STORE_VERIFICATION_FAILED);
        }

        verifyCertificateChain(certificates);
        X509Certificate signingCertificate = certificates.get(0);
        boolean verified = jwsObject.verify(new ECDSAVerifier((ECPublicKey) signingCertificate.getPublicKey()));
        if (!verified) {
            throw new ErrorHandler(ErrorStatus.PAYMENT_STORE_VERIFICATION_FAILED);
        }
    }

    private List<X509Certificate> readX5cCertificates(JWSHeader header) throws Exception {
        List<com.nimbusds.jose.util.Base64> encodedCertificates = header.getX509CertChain();
        if (encodedCertificates == null || encodedCertificates.isEmpty()) {
            throw new ErrorHandler(ErrorStatus.PAYMENT_STORE_VERIFICATION_FAILED);
        }

        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        List<X509Certificate> certificates = new ArrayList<>();
        for (com.nimbusds.jose.util.Base64 encodedCertificate : encodedCertificates) {
            byte[] certificateBytes = encodedCertificate.decode();
            certificates.add((X509Certificate) certificateFactory.generateCertificate(
                    new ByteArrayInputStream(certificateBytes)
            ));
        }
        return certificates;
    }

    private void verifyCertificateChain(List<X509Certificate> certificates) throws Exception {
        for (int i = 0; i < certificates.size(); i++) {
            certificates.get(i).checkValidity();
        }

        for (int i = 0; i < certificates.size() - 1; i++) {
            X509Certificate current = certificates.get(i);
            X509Certificate issuer = certificates.get(i + 1);
            if (!current.getIssuerX500Principal().equals(issuer.getSubjectX500Principal())) {
                throw new ErrorHandler(ErrorStatus.PAYMENT_STORE_VERIFICATION_FAILED);
            }
            current.verify(issuer.getPublicKey());
        }

        X509Certificate root = certificates.get(certificates.size() - 1);
        root.verify(root.getPublicKey());
        String rootFingerprint = sha256Hex(root.getEncoded());
        if (!TRUSTED_APPLE_ROOT_SHA256_FINGERPRINTS.contains(rootFingerprint)) {
            throw new ErrorHandler(ErrorStatus.PAYMENT_STORE_VERIFICATION_FAILED);
        }
    }

    private String sha256Hex(byte[] value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(value)).toUpperCase();
    }

    private void assertTransactionMatches(ApplePurchaseVerifyCommand command, JsonNode transaction) {
        if (!command.transactionId().equals(transaction.path("transactionId").asText())) {
            throw new ErrorHandler(ErrorStatus.PAYMENT_STORE_VERIFICATION_FAILED);
        }
        if (!command.productId().equals(transaction.path("productId").asText())) {
            throw new ErrorHandler(ErrorStatus.PAYMENT_PRODUCT_MISMATCH);
        }
        if (!command.bundleId().equals(transaction.path("bundleId").asText())) {
            throw new ErrorHandler(ErrorStatus.PAYMENT_PURCHASE_OWNER_MISMATCH);
        }
        if (!normalizeEnvironment(command.environment()).equals(normalizeEnvironment(transaction.path("environment").asText()))) {
            throw new ErrorHandler(ErrorStatus.PAYMENT_PURCHASE_OWNER_MISMATCH);
        }
        if (hasField(transaction, "revocationDate")) {
            throw new ErrorHandler(ErrorStatus.PAYMENT_PURCHASE_REVOKED);
        }
    }

    private LocalDateTime readMillisTime(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        long millis = value.isNumber() ? value.asLong() : Long.parseLong(value.asText());
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC);
    }

    private Long readApplePriceAsMicros(JsonNode transaction) {
        JsonNode price = transaction.get("price");
        if (price == null || price.isNull()) {
            return null;
        }
        return price.asLong() * 1000;
    }

    private boolean hasField(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field != null && !field.isNull();
    }

    private String firstText(JsonNode root, String fieldName, String fallback) {
        String value = root.path(fieldName).asText(null);
        return isBlank(value) ? fallback : value;
    }

    private String normalizeEnvironment(String value) {
        if (isBlank(value)) {
            return "";
        }
        return value.trim().replace("-", "_").toUpperCase();
    }

    private RuntimeException statusError(int statusCode) {
        if (statusCode >= 500 || statusCode == 401 || statusCode == 403) {
            return new ErrorHandler(ErrorStatus.PAYMENT_STORE_API_FAILED);
        }
        return new ErrorHandler(ErrorStatus.PAYMENT_STORE_VERIFICATION_FAILED);
    }

    private String baseUrl() {
        String environment = normalizeEnvironment(paymentProperties.getApple().getEnvironment());
        return "PRODUCTION".equals(environment) ? PRODUCTION_BASE_URL : SANDBOX_BASE_URL;
    }

    private HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(paymentProperties.getIap().getVerifyTimeoutMs()))
                .build();
    }

    private boolean isSuccess(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record TransactionPayload(
            String source,
            JsonNode transaction,
            JsonNode serverResponse
    ) {
    }
}
