package com.melissa.diary.service.payment;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.config.PaymentProperties;
import com.melissa.diary.domain.Entitlement;
import com.melissa.diary.domain.Payment;
import com.melissa.diary.domain.PaymentProduct;
import com.melissa.diary.domain.User;
import com.melissa.diary.domain.enums.EntitlementType;
import com.melissa.diary.domain.enums.PaymentPlatform;
import com.melissa.diary.domain.enums.ProductType;
import com.melissa.diary.repository.EntitlementRepository;
import com.melissa.diary.repository.PaymentProductRepository;
import com.melissa.diary.repository.PaymentRepository;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.security.EncryptionManager;
import com.melissa.diary.service.payment.store.VerifiedPurchase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentGrantServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentProductRepository paymentProductRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private EntitlementRepository entitlementRepository;

    @Mock
    private EncryptionManager encryptionManager;

    private PaymentGrantService paymentGrantService;

    @BeforeEach
    void setUp() {
        PaymentProperties paymentProperties = new PaymentProperties();
        paymentProperties.getGoogle().setTokenHashSalt("test-salt");
        paymentGrantService = new PaymentGrantService(
                paymentProperties,
                userRepository,
                paymentProductRepository,
                paymentRepository,
                entitlementRepository,
                encryptionManager
        );
    }

    @Test
    void grantGooglePurchaseCreatesPaymentAndEntitlement() {
        Long userId = 1L;
        User user = user(userId);
        PaymentProduct product = removeAdsProduct(PaymentPlatform.GOOGLE, "premium");
        VerifiedPurchase purchase = googlePurchase("premium");

        when(paymentRepository.findByPlatformAndGooglePurchaseTokenHashForUpdate(PaymentPlatform.GOOGLE, paymentGrantService.hashGooglePurchaseToken("token-1")))
                .thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(paymentProductRepository.findByPlatformAndStoreProductIdAndActiveTrue(PaymentPlatform.GOOGLE, "premium"))
                .thenReturn(Optional.of(product));
        when(encryptionManager.encrypt("token-1")).thenReturn("encrypted-token");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(100L);
            return payment;
        });
        when(entitlementRepository.findByUserIdAndEntitlementTypeForUpdate(userId, EntitlementType.REMOVE_ADS))
                .thenReturn(Optional.empty());
        when(entitlementRepository.save(any(Entitlement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentGrantResult result = paymentGrantService.grantGooglePurchase(userId, purchase, "token-1");

        assertThat(result.isAlreadyProcessed()).isFalse();
        assertThat(result.getPayment().getId()).isEqualTo(100L);
        assertThat(result.getPayment().getGooglePurchaseTokenHash()).hasSize(64);
        assertThat(result.getPayment().getGooglePurchaseTokenEncrypted()).isEqualTo("encrypted-token");
        assertThat(result.getEntitlement().getActive()).isTrue();
        assertThat(result.getEntitlement().getEntitlementType()).isEqualTo(EntitlementType.REMOVE_ADS);
        verify(entitlementRepository).save(any(Entitlement.class));
    }

    @Test
    void grantGooglePurchaseRejectsPurchaseAlreadyOwnedByAnotherUser() {
        Long userId = 1L;
        User otherUser = user(2L);
        Payment existing = Payment.builder()
                .user(otherUser)
                .platform(PaymentPlatform.GOOGLE)
                .product(removeAdsProduct(PaymentPlatform.GOOGLE, "premium"))
                .storeProductId("premium")
                .productType(ProductType.NON_CONSUMABLE)
                .build();

        when(paymentRepository.findByPlatformAndGooglePurchaseTokenHashForUpdate(PaymentPlatform.GOOGLE, paymentGrantService.hashGooglePurchaseToken("token-1")))
                .thenReturn(Optional.of(existing));

        ErrorHandler error = assertThrows(
                ErrorHandler.class,
                () -> paymentGrantService.grantGooglePurchase(userId, googlePurchase("premium"), "token-1")
        );

        assertThat(error.getErrorCode()).isEqualTo(ErrorStatus.PAYMENT_ALREADY_OWNED_BY_OTHER_USER);
    }

    private User user(Long id) {
        return User.builder()
                .id(id)
                .provider("GOOGLE")
                .nickname("user")
                .build();
    }

    private PaymentProduct removeAdsProduct(PaymentPlatform platform, String storeProductId) {
        return PaymentProduct.builder()
                .id(10L)
                .productId("remove_ads")
                .platform(platform)
                .storeProductId(storeProductId)
                .productType(ProductType.NON_CONSUMABLE)
                .entitlementType(EntitlementType.REMOVE_ADS)
                .active(true)
                .displayName("광고 제거")
                .build();
    }

    private VerifiedPurchase googlePurchase(String productId) {
        return VerifiedPurchase.builder()
                .platform(PaymentPlatform.GOOGLE)
                .storeProductId(productId)
                .productType(ProductType.NON_CONSUMABLE)
                .orderId("GPA.1")
                .purchasedAt(LocalDateTime.of(2026, 6, 15, 10, 0))
                .rawPayload("{}")
                .build();
    }
}
