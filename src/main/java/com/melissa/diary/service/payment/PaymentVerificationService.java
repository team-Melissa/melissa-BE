package com.melissa.diary.service.payment;

import com.melissa.diary.apiPayload.code.ErrorReasonDTO;
import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.GeneralException;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.config.PaymentProperties;
import com.melissa.diary.domain.Entitlement;
import com.melissa.diary.domain.Payment;
import com.melissa.diary.domain.enums.EntitlementType;
import com.melissa.diary.repository.EntitlementRepository;
import com.melissa.diary.service.payment.store.ApplePurchaseVerifyCommand;
import com.melissa.diary.service.payment.store.AppleStorePurchaseVerifier;
import com.melissa.diary.service.payment.store.GoogleAcknowledgeCommand;
import com.melissa.diary.service.payment.store.GooglePlayPurchaseVerifier;
import com.melissa.diary.service.payment.store.GooglePurchaseVerifyCommand;
import com.melissa.diary.service.payment.store.VerifiedPurchase;
import com.melissa.diary.web.dto.EntitlementResponseDTO;
import com.melissa.diary.web.dto.PaymentRequestDTO;
import com.melissa.diary.web.dto.PaymentResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentVerificationService {

    private final PaymentProperties paymentProperties;
    private final GooglePlayPurchaseVerifier googlePlayPurchaseVerifier;
    private final AppleStorePurchaseVerifier appleStorePurchaseVerifier;
    private final PaymentGrantService paymentGrantService;
    private final EntitlementRepository entitlementRepository;

    public PaymentResponseDTO.VerifyResponse verifyGoogle(
            Long userId,
            PaymentRequestDTO.GoogleVerifyRequest request
    ) {
        assertIapEnabled();
        assertGoogleEnabled();
        assertEquals(paymentProperties.getGoogle().getPackageName(), request.getPackageName(),
                ErrorStatus.PAYMENT_PURCHASE_OWNER_MISMATCH);
        assertEquals(paymentProperties.getGoogle().getProductRemoveAds(), request.getProductId(),
                ErrorStatus.PAYMENT_PRODUCT_MISMATCH);

        VerifiedPurchase verified = googlePlayPurchaseVerifier.verify(new GooglePurchaseVerifyCommand(
                request.getPackageName(),
                request.getProductId(),
                request.getPurchaseToken(),
                request.getOrderId()
        ));

        PaymentGrantResult grantResult = paymentGrantService.grantGooglePurchase(
                userId,
                verified,
                request.getPurchaseToken()
        );

        boolean acknowledged = isAcknowledged(grantResult.getPayment());
        if (!acknowledged) {
            acknowledged = acknowledgeGooglePurchase(request, verified, grantResult);
        }

        return toVerifyResponse(grantResult, acknowledged, null);
    }

    public PaymentResponseDTO.VerifyResponse verifyApple(
            Long userId,
            PaymentRequestDTO.AppleVerifyRequest request
    ) {
        assertIapEnabled();
        assertAppleEnabled();
        assertEquals(paymentProperties.getApple().getBundleId(), request.getBundleId(),
                ErrorStatus.PAYMENT_PURCHASE_OWNER_MISMATCH);
        assertEquals(paymentProperties.getApple().getProductRemoveAds(), request.getProductId(),
                ErrorStatus.PAYMENT_PRODUCT_MISMATCH);
        assertEquals(normalizeEnvironment(paymentProperties.getApple().getEnvironment()), normalizeEnvironment(request.getEnvironment()),
                ErrorStatus.PAYMENT_PURCHASE_OWNER_MISMATCH);

        VerifiedPurchase verified = appleStorePurchaseVerifier.verify(new ApplePurchaseVerifyCommand(
                request.getProductId(),
                request.getTransactionId(),
                request.getOriginalTransactionId(),
                request.getEnvironment(),
                request.getBundleId()
        ));

        PaymentGrantResult grantResult = paymentGrantService.grantApplePurchase(userId, verified);
        return toVerifyResponse(grantResult, null, true);
    }

    public PaymentResponseDTO.RestoreResponse restoreGoogle(
            Long userId,
            PaymentRequestDTO.GoogleRestoreRequest request
    ) {
        List<PaymentResponseDTO.RestoredPurchase> restored = new ArrayList<>();
        List<PaymentResponseDTO.FailedPurchase> failed = new ArrayList<>();

        for (PaymentRequestDTO.GoogleVerifyRequest purchase : request.getPurchases()) {
            try {
                PaymentResponseDTO.VerifyResponse response = verifyGoogle(userId, purchase);
                restored.add(toRestoredPurchase(response));
            } catch (RuntimeException e) {
                failed.add(toFailedPurchase("GOOGLE", purchase.getProductId(), safeIdentifier(purchase.getOrderId()), e));
            }
        }

        return PaymentResponseDTO.RestoreResponse.builder()
                .restored(restored)
                .failed(failed)
                .features(featureSummary(userId))
                .build();
    }

    public PaymentResponseDTO.RestoreResponse restoreApple(
            Long userId,
            PaymentRequestDTO.AppleRestoreRequest request
    ) {
        List<PaymentResponseDTO.RestoredPurchase> restored = new ArrayList<>();
        List<PaymentResponseDTO.FailedPurchase> failed = new ArrayList<>();

        for (PaymentRequestDTO.AppleVerifyRequest transaction : request.getTransactions()) {
            try {
                PaymentResponseDTO.VerifyResponse response = verifyApple(userId, transaction);
                restored.add(toRestoredPurchase(response));
            } catch (RuntimeException e) {
                failed.add(toFailedPurchase("APPLE", transaction.getProductId(), transaction.getTransactionId(), e));
            }
        }

        return PaymentResponseDTO.RestoreResponse.builder()
                .restored(restored)
                .failed(failed)
                .features(featureSummary(userId))
                .build();
    }

    private boolean acknowledgeGooglePurchase(
            PaymentRequestDTO.GoogleVerifyRequest request,
            VerifiedPurchase verified,
            PaymentGrantResult grantResult
    ) {
        Long paymentId = grantResult.getPayment().getId();
        if (verified.isAcknowledged()) {
            paymentGrantService.markGoogleAcknowledged(paymentId);
            return true;
        }

        try {
            googlePlayPurchaseVerifier.acknowledge(new GoogleAcknowledgeCommand(
                    request.getPackageName(),
                    request.getProductId(),
                    request.getPurchaseToken()
            ));
            paymentGrantService.markGoogleAcknowledged(paymentId);
            return true;
        } catch (RuntimeException e) {
            log.warn("[PaymentVerificationService] Google acknowledge will be retried. paymentId={}", paymentId, e);
            paymentGrantService.markGoogleAcknowledgeFailed(paymentId, e);
            return false;
        }
    }

    private PaymentResponseDTO.VerifyResponse toVerifyResponse(
            PaymentGrantResult grantResult,
            Boolean acknowledged,
            Boolean finishRequired
    ) {
        Payment payment = grantResult.getPayment();
        Entitlement entitlement = grantResult.getEntitlement();

        return PaymentResponseDTO.VerifyResponse.builder()
                .paymentId(payment.getId())
                .platform(payment.getPlatform().name())
                .productId(payment.getProduct().getProductId())
                .status(payment.getStatus().name())
                .entitlement(PaymentResponseDTO.EntitlementSummary.builder()
                        .type(entitlement == null ? null : entitlement.getEntitlementType().name())
                        .active(entitlement != null && Boolean.TRUE.equals(entitlement.getActive()))
                        .build())
                .acknowledged(acknowledged)
                .finishRequired(finishRequired)
                .alreadyProcessed(grantResult.isAlreadyProcessed())
                .purchasedAt(payment.getPurchasedAt())
                .verifiedAt(payment.getVerifiedAt())
                .build();
    }

    private PaymentResponseDTO.RestoredPurchase toRestoredPurchase(PaymentResponseDTO.VerifyResponse response) {
        return PaymentResponseDTO.RestoredPurchase.builder()
                .paymentId(response.getPaymentId())
                .platform(response.getPlatform())
                .productId(response.getProductId())
                .status(response.getStatus())
                .entitlementType(response.getEntitlement() == null ? null : response.getEntitlement().getType())
                .build();
    }

    private PaymentResponseDTO.FailedPurchase toFailedPurchase(
            String platform,
            String productId,
            String identifier,
            RuntimeException exception
    ) {
        ErrorReasonDTO reason = exception instanceof GeneralException generalException
                ? generalException.getErrorReasonHttpStatus()
                : ErrorStatus._INTERNAL_SERVER_ERROR.getReasonHttpStatus();

        return PaymentResponseDTO.FailedPurchase.builder()
                .platform(platform)
                .productId(productId)
                .identifier(identifier)
                .code(reason.getCode())
                .message(reason.getMessage())
                .build();
    }

    private EntitlementResponseDTO.FeatureSummary featureSummary(Long userId) {
        boolean adRemoved = entitlementRepository.findByUserIdAndActiveTrue(userId).stream()
                .anyMatch(entitlement -> entitlement.getEntitlementType() == EntitlementType.REMOVE_ADS);
        return EntitlementResponseDTO.FeatureSummary.builder()
                .adRemoved(adRemoved)
                .build();
    }

    private void assertIapEnabled() {
        if (!paymentProperties.getIap().isEnabled()) {
            throw new ErrorHandler(ErrorStatus.PAYMENT_DISABLED);
        }
    }

    private void assertGoogleEnabled() {
        if (!paymentProperties.getGoogle().isEnabled()) {
            throw new ErrorHandler(ErrorStatus.PAYMENT_DISABLED);
        }
    }

    private void assertAppleEnabled() {
        if (!paymentProperties.getApple().isEnabled()) {
            throw new ErrorHandler(ErrorStatus.PAYMENT_DISABLED);
        }
    }

    private void assertEquals(String expected, String actual, ErrorStatus errorStatus) {
        if (!normalize(expected).equals(normalize(actual))) {
            throw new ErrorHandler(errorStatus);
        }
    }

    private boolean isAcknowledged(Payment payment) {
        return payment.getAcknowledgedAt() != null;
    }

    private String safeIdentifier(String orderId) {
        return isBlank(orderId) ? "purchaseToken" : orderId;
    }

    private String normalizeEnvironment(String value) {
        return normalize(value).replace("-", "_");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
