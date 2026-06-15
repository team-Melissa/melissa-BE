package com.melissa.diary.service.payment;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.config.PaymentProperties;
import com.melissa.diary.domain.Entitlement;
import com.melissa.diary.domain.Payment;
import com.melissa.diary.domain.PaymentProduct;
import com.melissa.diary.domain.User;
import com.melissa.diary.domain.enums.EntitlementSourceType;
import com.melissa.diary.domain.enums.EntitlementType;
import com.melissa.diary.domain.enums.PaymentPlatform;
import com.melissa.diary.domain.enums.PaymentStatus;
import com.melissa.diary.repository.EntitlementRepository;
import com.melissa.diary.repository.PaymentProductRepository;
import com.melissa.diary.repository.PaymentRepository;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.security.EncryptionManager;
import com.melissa.diary.service.payment.store.VerifiedPurchase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class PaymentGrantService {

    private final PaymentProperties paymentProperties;
    private final UserRepository userRepository;
    private final PaymentProductRepository paymentProductRepository;
    private final PaymentRepository paymentRepository;
    private final EntitlementRepository entitlementRepository;
    private final EncryptionManager encryptionManager;

    @Transactional
    public PaymentGrantResult grantGooglePurchase(Long userId, VerifiedPurchase purchase, String purchaseToken) {
        if (purchase.isRevoked()) {
            throw new ErrorHandler(ErrorStatus.PAYMENT_PURCHASE_REVOKED);
        }

        String tokenHash = hashGooglePurchaseToken(purchaseToken);
        return paymentRepository
                .findByPlatformAndGooglePurchaseTokenHashForUpdate(PaymentPlatform.GOOGLE, tokenHash)
                .map(existing -> existingGrantResult(userId, existing))
                .orElseGet(() -> createGooglePayment(userId, purchase, purchaseToken, tokenHash));
    }

    @Transactional
    public PaymentGrantResult grantApplePurchase(Long userId, VerifiedPurchase purchase) {
        if (purchase.isRevoked()) {
            throw new ErrorHandler(ErrorStatus.PAYMENT_PURCHASE_REVOKED);
        }

        return paymentRepository
                .findByPlatformAndAppleTransactionIdForUpdate(PaymentPlatform.APPLE, purchase.getAppleTransactionId())
                .map(existing -> existingGrantResult(userId, existing))
                .orElseGet(() -> createApplePayment(userId, purchase));
    }

    @Transactional
    public void markGoogleAcknowledged(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.PAYMENT_PRODUCT_NOT_FOUND));
        payment.setAcknowledgedAt(LocalDateTime.now());
        payment.setFailureReason(null);
    }

    @Transactional
    public void markGoogleAcknowledgeFailed(Long paymentId, Exception exception) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.PAYMENT_PRODUCT_NOT_FOUND));
        payment.setFailureReason(truncate("Google acknowledge failed: " + exception.getMessage(), 500));
    }

    String hashGooglePurchaseToken(String purchaseToken) {
        String salt = paymentProperties.getGoogle().getTokenHashSalt();
        if (isBlank(salt)) {
            throw new ErrorHandler(ErrorStatus.PAYMENT_GRANT_FAILED);
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((salt + ":" + purchaseToken).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception e) {
            throw new ErrorHandler(ErrorStatus.PAYMENT_GRANT_FAILED);
        }
    }

    private PaymentGrantResult createGooglePayment(
            Long userId,
            VerifiedPurchase purchase,
            String purchaseToken,
            String tokenHash
    ) {
        User user = findUser(userId);
        PaymentProduct product = findProduct(PaymentPlatform.GOOGLE, purchase.getStoreProductId());
        LocalDateTime now = LocalDateTime.now();

        Payment payment = Payment.builder()
                .user(user)
                .platform(PaymentPlatform.GOOGLE)
                .product(product)
                .storeProductId(product.getStoreProductId())
                .productType(product.getProductType())
                .status(PaymentStatus.PURCHASED)
                .amountMicros(purchase.getAmountMicros())
                .currency(purchase.getCurrency())
                .googlePurchaseTokenHash(tokenHash)
                .googlePurchaseTokenEncrypted(encryptionManager.encrypt(purchaseToken))
                .googleOrderId(firstNonBlank(purchase.getOrderId(), null))
                .purchasedAt(purchase.getPurchasedAt())
                .verifiedAt(now)
                .acknowledgedAt(purchase.isAcknowledged() ? now : null)
                .rawVerifiedPayload(purchase.getRawPayload())
                .build();
        Payment saved = paymentRepository.save(payment);
        Entitlement entitlement = upsertEntitlement(user, product, saved, now);

        return PaymentGrantResult.builder()
                .payment(saved)
                .entitlement(entitlement)
                .alreadyProcessed(false)
                .build();
    }

    private PaymentGrantResult createApplePayment(Long userId, VerifiedPurchase purchase) {
        User user = findUser(userId);
        PaymentProduct product = findProduct(PaymentPlatform.APPLE, purchase.getStoreProductId());
        LocalDateTime now = LocalDateTime.now();

        Payment payment = Payment.builder()
                .user(user)
                .platform(PaymentPlatform.APPLE)
                .product(product)
                .storeProductId(product.getStoreProductId())
                .productType(product.getProductType())
                .status(PaymentStatus.PURCHASED)
                .amountMicros(purchase.getAmountMicros())
                .currency(purchase.getCurrency())
                .appleTransactionId(purchase.getAppleTransactionId())
                .appleOriginalTransactionId(purchase.getAppleOriginalTransactionId())
                .appleEnvironment(purchase.getAppleEnvironment())
                .purchasedAt(purchase.getPurchasedAt())
                .verifiedAt(now)
                .rawVerifiedPayload(purchase.getRawPayload())
                .build();
        Payment saved = paymentRepository.save(payment);
        Entitlement entitlement = upsertEntitlement(user, product, saved, now);

        return PaymentGrantResult.builder()
                .payment(saved)
                .entitlement(entitlement)
                .alreadyProcessed(false)
                .build();
    }

    private PaymentGrantResult existingGrantResult(Long userId, Payment existing) {
        if (!existing.getUser().getId().equals(userId)) {
            throw new ErrorHandler(ErrorStatus.PAYMENT_ALREADY_OWNED_BY_OTHER_USER);
        }

        Entitlement entitlement = null;
        EntitlementType entitlementType = existing.getProduct().getEntitlementType();
        if (entitlementType != null) {
            entitlement = entitlementRepository
                    .findByUserIdAndEntitlementTypeForUpdate(userId, entitlementType)
                    .orElse(null);
        }

        return PaymentGrantResult.builder()
                .payment(existing)
                .entitlement(entitlement)
                .alreadyProcessed(true)
                .build();
    }

    private Entitlement upsertEntitlement(User user, PaymentProduct product, Payment payment, LocalDateTime now) {
        EntitlementType entitlementType = product.getEntitlementType();
        if (entitlementType == null) {
            throw new ErrorHandler(ErrorStatus.PAYMENT_GRANT_FAILED);
        }

        Entitlement entitlement = entitlementRepository
                .findByUserIdAndEntitlementTypeForUpdate(user.getId(), entitlementType)
                .orElseGet(() -> Entitlement.builder()
                        .user(user)
                        .entitlementType(entitlementType)
                        .build());

        entitlement.setActive(true);
        entitlement.setSourceType(EntitlementSourceType.PAYMENT);
        entitlement.setSourcePlatform(payment.getPlatform());
        entitlement.setSourcePayment(payment);
        entitlement.setGrantedAt(now);
        entitlement.setRevokedAt(null);
        entitlement.setRevocationReason(null);

        return entitlementRepository.save(entitlement);
    }

    private PaymentProduct findProduct(PaymentPlatform platform, String storeProductId) {
        return paymentProductRepository
                .findByPlatformAndStoreProductIdAndActiveTrue(platform, storeProductId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.PAYMENT_PRODUCT_NOT_FOUND));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));
    }

    private String firstNonBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
