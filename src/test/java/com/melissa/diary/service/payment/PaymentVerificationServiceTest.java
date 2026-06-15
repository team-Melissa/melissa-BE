package com.melissa.diary.service.payment;

import com.melissa.diary.config.PaymentProperties;
import com.melissa.diary.domain.Entitlement;
import com.melissa.diary.domain.Payment;
import com.melissa.diary.domain.PaymentProduct;
import com.melissa.diary.domain.User;
import com.melissa.diary.domain.enums.EntitlementSourceType;
import com.melissa.diary.domain.enums.EntitlementType;
import com.melissa.diary.domain.enums.PaymentPlatform;
import com.melissa.diary.domain.enums.PaymentStatus;
import com.melissa.diary.domain.enums.ProductType;
import com.melissa.diary.repository.EntitlementRepository;
import com.melissa.diary.service.payment.store.ApplePurchaseVerifyCommand;
import com.melissa.diary.service.payment.store.AppleStorePurchaseVerifier;
import com.melissa.diary.service.payment.store.GoogleAcknowledgeCommand;
import com.melissa.diary.service.payment.store.GooglePlayPurchaseVerifier;
import com.melissa.diary.service.payment.store.GooglePurchaseVerifyCommand;
import com.melissa.diary.service.payment.store.VerifiedPurchase;
import com.melissa.diary.web.dto.PaymentRequestDTO;
import com.melissa.diary.web.dto.PaymentResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentVerificationServiceTest {

    @Mock
    private GooglePlayPurchaseVerifier googlePlayPurchaseVerifier;

    @Mock
    private AppleStorePurchaseVerifier appleStorePurchaseVerifier;

    @Mock
    private PaymentGrantService paymentGrantService;

    @Mock
    private EntitlementRepository entitlementRepository;

    private PaymentVerificationService paymentVerificationService;

    @BeforeEach
    void setUp() {
        PaymentProperties paymentProperties = new PaymentProperties();
        paymentProperties.getIap().setEnabled(true);
        paymentProperties.getGoogle().setEnabled(true);
        paymentProperties.getApple().setEnabled(true);
        paymentProperties.getGoogle().setPackageName("com.melissa.melissaFE");
        paymentProperties.getGoogle().setProductRemoveAds("premium");
        paymentProperties.getApple().setBundleId("com.melissa.melissaFE");
        paymentProperties.getApple().setProductRemoveAds("com.melissa.melissaFE.premium");
        paymentProperties.getApple().setEnvironment("SANDBOX");

        paymentVerificationService = new PaymentVerificationService(
                paymentProperties,
                googlePlayPurchaseVerifier,
                appleStorePurchaseVerifier,
                paymentGrantService,
                entitlementRepository
        );
    }

    @Test
    void verifyGoogleGrantsEntitlementThenAcknowledgesPurchase() {
        Long userId = 1L;
        PaymentRequestDTO.GoogleVerifyRequest request = new PaymentRequestDTO.GoogleVerifyRequest(
                "premium",
                "com.melissa.melissaFE",
                "token-1",
                "GPA.1"
        );
        VerifiedPurchase verified = verifiedGoogle(false);
        PaymentGrantResult grantResult = grantResult(PaymentPlatform.GOOGLE, false, null);

        when(googlePlayPurchaseVerifier.verify(any(GooglePurchaseVerifyCommand.class))).thenReturn(verified);
        when(paymentGrantService.grantGooglePurchase(userId, verified, "token-1")).thenReturn(grantResult);

        PaymentResponseDTO.VerifyResponse response = paymentVerificationService.verifyGoogle(userId, request);

        assertThat(response.getAcknowledged()).isTrue();
        assertThat(response.getEntitlement().getActive()).isTrue();
        assertThat(response.getAlreadyProcessed()).isFalse();
        verify(googlePlayPurchaseVerifier).acknowledge(any(GoogleAcknowledgeCommand.class));
        verify(paymentGrantService).markGoogleAcknowledged(100L);
    }

    @Test
    void verifyGoogleKeepsGrantWhenAcknowledgeFails() {
        Long userId = 1L;
        PaymentRequestDTO.GoogleVerifyRequest request = new PaymentRequestDTO.GoogleVerifyRequest(
                "premium",
                "com.melissa.melissaFE",
                "token-1",
                "GPA.1"
        );
        VerifiedPurchase verified = verifiedGoogle(false);
        PaymentGrantResult grantResult = grantResult(PaymentPlatform.GOOGLE, false, null);

        when(googlePlayPurchaseVerifier.verify(any(GooglePurchaseVerifyCommand.class))).thenReturn(verified);
        when(paymentGrantService.grantGooglePurchase(userId, verified, "token-1")).thenReturn(grantResult);
        doThrow(new RuntimeException("ack failed"))
                .when(googlePlayPurchaseVerifier).acknowledge(any(GoogleAcknowledgeCommand.class));

        PaymentResponseDTO.VerifyResponse response = paymentVerificationService.verifyGoogle(userId, request);

        assertThat(response.getAcknowledged()).isFalse();
        assertThat(response.getEntitlement().getActive()).isTrue();
        verify(paymentGrantService).markGoogleAcknowledgeFailed(any(), any(RuntimeException.class));
    }

    @Test
    void verifyAppleReturnsFinishRequiredAfterServerVerification() {
        Long userId = 1L;
        PaymentRequestDTO.AppleVerifyRequest request = new PaymentRequestDTO.AppleVerifyRequest(
                "com.melissa.melissaFE.premium",
                "2000000123456789",
                "2000000123456789",
                "SANDBOX",
                "com.melissa.melissaFE"
        );
        VerifiedPurchase verified = verifiedApple();
        PaymentGrantResult grantResult = grantResult(PaymentPlatform.APPLE, true, null);

        when(appleStorePurchaseVerifier.verify(any(ApplePurchaseVerifyCommand.class))).thenReturn(verified);
        when(paymentGrantService.grantApplePurchase(userId, verified)).thenReturn(grantResult);

        PaymentResponseDTO.VerifyResponse response = paymentVerificationService.verifyApple(userId, request);

        assertThat(response.getFinishRequired()).isTrue();
        assertThat(response.getAlreadyProcessed()).isTrue();
        assertThat(response.getPlatform()).isEqualTo("APPLE");
    }

    @Test
    void restoreGoogleReturnsFeatureSummaryAfterRestoredPurchase() {
        Long userId = 1L;
        PaymentRequestDTO.GoogleRestoreRequest request = new PaymentRequestDTO.GoogleRestoreRequest(List.of(
                new PaymentRequestDTO.GoogleVerifyRequest("premium", "com.melissa.melissaFE", "token-1", "GPA.1")
        ));
        VerifiedPurchase verified = verifiedGoogle(true);
        PaymentGrantResult grantResult = grantResult(PaymentPlatform.GOOGLE, true, LocalDateTime.now());

        when(googlePlayPurchaseVerifier.verify(any(GooglePurchaseVerifyCommand.class))).thenReturn(verified);
        when(paymentGrantService.grantGooglePurchase(userId, verified, "token-1")).thenReturn(grantResult);
        when(entitlementRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(grantResult.getEntitlement()));

        PaymentResponseDTO.RestoreResponse response = paymentVerificationService.restoreGoogle(userId, request);

        assertThat(response.getRestored()).hasSize(1);
        assertThat(response.getFailed()).isEmpty();
        assertThat(response.getFeatures().getAdRemoved()).isTrue();
    }

    private VerifiedPurchase verifiedGoogle(boolean acknowledged) {
        return VerifiedPurchase.builder()
                .platform(PaymentPlatform.GOOGLE)
                .storeProductId("premium")
                .productType(ProductType.NON_CONSUMABLE)
                .orderId("GPA.1")
                .purchasedAt(LocalDateTime.of(2026, 6, 15, 10, 0))
                .acknowledged(acknowledged)
                .rawPayload("{}")
                .build();
    }

    private VerifiedPurchase verifiedApple() {
        return VerifiedPurchase.builder()
                .platform(PaymentPlatform.APPLE)
                .storeProductId("com.melissa.melissaFE.premium")
                .productType(ProductType.NON_CONSUMABLE)
                .appleTransactionId("2000000123456789")
                .appleOriginalTransactionId("2000000123456789")
                .appleEnvironment("SANDBOX")
                .purchasedAt(LocalDateTime.of(2026, 6, 15, 10, 0))
                .rawPayload("{}")
                .build();
    }

    private PaymentGrantResult grantResult(PaymentPlatform platform, boolean alreadyProcessed, LocalDateTime acknowledgedAt) {
        User user = User.builder()
                .id(1L)
                .provider("GOOGLE")
                .nickname("user")
                .build();
        PaymentProduct product = PaymentProduct.builder()
                .id(10L)
                .productId("remove_ads")
                .platform(platform)
                .storeProductId(platform == PaymentPlatform.GOOGLE ? "premium" : "com.melissa.melissaFE.premium")
                .productType(ProductType.NON_CONSUMABLE)
                .entitlementType(EntitlementType.REMOVE_ADS)
                .displayName("광고 제거")
                .active(true)
                .build();
        Payment payment = Payment.builder()
                .id(100L)
                .user(user)
                .platform(platform)
                .product(product)
                .storeProductId(product.getStoreProductId())
                .productType(ProductType.NON_CONSUMABLE)
                .status(PaymentStatus.PURCHASED)
                .purchasedAt(LocalDateTime.of(2026, 6, 15, 10, 0))
                .verifiedAt(LocalDateTime.of(2026, 6, 15, 10, 1))
                .acknowledgedAt(acknowledgedAt)
                .build();
        Entitlement entitlement = Entitlement.builder()
                .user(user)
                .entitlementType(EntitlementType.REMOVE_ADS)
                .active(true)
                .sourceType(EntitlementSourceType.PAYMENT)
                .sourcePlatform(platform)
                .sourcePayment(payment)
                .grantedAt(LocalDateTime.of(2026, 6, 15, 10, 1))
                .build();

        return PaymentGrantResult.builder()
                .payment(payment)
                .entitlement(entitlement)
                .alreadyProcessed(alreadyProcessed)
                .build();
    }
}
