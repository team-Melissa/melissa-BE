package com.melissa.diary.service.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.domain.Entitlement;
import com.melissa.diary.domain.Payment;
import com.melissa.diary.domain.PaymentEvent;
import com.melissa.diary.domain.PaymentProduct;
import com.melissa.diary.domain.User;
import com.melissa.diary.domain.enums.EntitlementSourceType;
import com.melissa.diary.domain.enums.EntitlementType;
import com.melissa.diary.domain.enums.PaymentPlatform;
import com.melissa.diary.domain.enums.PaymentStatus;
import com.melissa.diary.domain.enums.ProductType;
import com.melissa.diary.repository.EntitlementRepository;
import com.melissa.diary.repository.PaymentEventRepository;
import com.melissa.diary.repository.PaymentRepository;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.service.AdminAuthorizationService;
import com.melissa.diary.web.dto.PaymentRequestDTO;
import com.melissa.diary.web.dto.PaymentResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentAdminRefundServiceTest {

    @Mock
    private AdminAuthorizationService adminAuthorizationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private EntitlementRepository entitlementRepository;

    @Mock
    private PaymentEventRepository paymentEventRepository;

    private PaymentAdminRefundService paymentAdminRefundService;

    @BeforeEach
    void setUp() {
        paymentAdminRefundService = new PaymentAdminRefundService(
                adminAuthorizationService,
                userRepository,
                paymentRepository,
                entitlementRepository,
                paymentEventRepository,
                new ObjectMapper()
        );
    }

    @Test
    void refundManuallyMarksPaymentRefundedAndRevokesEntitlement() {
        Long adminUserId = 1L;
        Long paymentId = 100L;
        LocalDateTime refundedAt = LocalDateTime.of(2026, 6, 16, 1, 0);
        User admin = user(adminUserId);
        User buyer = user(2L);
        Payment payment = purchasedPayment(paymentId, buyer);
        Entitlement entitlement = activeEntitlement(buyer, payment);
        PaymentRequestDTO.AdminManualRefundRequest request =
                new PaymentRequestDTO.AdminManualRefundRequest("Google Play Console refund confirmed", refundedAt);

        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(admin));
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
        when(entitlementRepository.findBySourcePaymentIdForUpdate(paymentId)).thenReturn(Optional.of(entitlement));
        when(paymentEventRepository.findByPlatformAndEventId(PaymentPlatform.GOOGLE, "MANUAL_REFUND:" + paymentId))
                .thenReturn(Optional.empty());

        PaymentResponseDTO.AdminManualRefundResponse response =
                paymentAdminRefundService.refundManually(adminUserId, paymentId, request);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getRefundedAt()).isEqualTo(refundedAt);
        assertThat(entitlement.getActive()).isFalse();
        assertThat(entitlement.getRevokedAt()).isEqualTo(refundedAt);
        assertThat(entitlement.getRevocationReason()).isEqualTo("Google Play Console refund confirmed");
        assertThat(response.getPaymentId()).isEqualTo(paymentId);
        assertThat(response.getUserId()).isEqualTo(buyer.getId());
        assertThat(response.getStatus()).isEqualTo("REFUNDED");
        assertThat(response.getEntitlementRevoked()).isTrue();
        assertThat(response.getAlreadyProcessed()).isFalse();

        ArgumentCaptor<PaymentEvent> eventCaptor = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(paymentEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventId()).isEqualTo("MANUAL_REFUND:" + paymentId);
        assertThat(eventCaptor.getValue().getActorUser()).isEqualTo(admin);
    }

    @Test
    void refundManuallyReturnsAlreadyProcessedWhenPaymentWasRefunded() {
        Long adminUserId = 1L;
        Long paymentId = 100L;
        LocalDateTime previousRefundedAt = LocalDateTime.of(2026, 6, 15, 1, 0);
        User admin = user(adminUserId);
        User buyer = user(2L);
        Payment payment = purchasedPayment(paymentId, buyer);
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(previousRefundedAt);
        Entitlement entitlement = activeEntitlement(buyer, payment);
        entitlement.setActive(false);
        entitlement.setRevokedAt(previousRefundedAt);

        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(admin));
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
        when(entitlementRepository.findBySourcePaymentIdForUpdate(paymentId)).thenReturn(Optional.of(entitlement));

        PaymentResponseDTO.AdminManualRefundResponse response =
                paymentAdminRefundService.refundManually(
                        adminUserId,
                        paymentId,
                        new PaymentRequestDTO.AdminManualRefundRequest("duplicate request", null)
                );

        assertThat(response.getAlreadyProcessed()).isTrue();
        assertThat(response.getRefundedAt()).isEqualTo(previousRefundedAt);
        assertThat(response.getEntitlementRevoked()).isTrue();
        verify(paymentEventRepository, never()).save(any(PaymentEvent.class));
    }

    @Test
    void refundManuallyRevokesActiveEntitlementEvenWhenPaymentWasAlreadyRefunded() {
        Long adminUserId = 1L;
        Long paymentId = 100L;
        LocalDateTime previousRefundedAt = LocalDateTime.of(2026, 6, 15, 1, 0);
        User admin = user(adminUserId);
        User buyer = user(2L);
        Payment payment = purchasedPayment(paymentId, buyer);
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(previousRefundedAt);
        Entitlement entitlement = activeEntitlement(buyer, payment);

        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(admin));
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
        when(entitlementRepository.findBySourcePaymentIdForUpdate(paymentId)).thenReturn(Optional.of(entitlement));
        when(paymentEventRepository.findByPlatformAndEventId(PaymentPlatform.GOOGLE, "MANUAL_REFUND:" + paymentId))
                .thenReturn(Optional.empty());

        PaymentResponseDTO.AdminManualRefundResponse response =
                paymentAdminRefundService.refundManually(
                        adminUserId,
                        paymentId,
                        new PaymentRequestDTO.AdminManualRefundRequest("fix active entitlement", null)
                );

        assertThat(response.getAlreadyProcessed()).isTrue();
        assertThat(response.getEntitlementRevoked()).isTrue();
        assertThat(entitlement.getActive()).isFalse();
        assertThat(entitlement.getRevokedAt()).isEqualTo(previousRefundedAt);
        verify(paymentEventRepository).save(any(PaymentEvent.class));
    }

    @Test
    void refundManuallyRejectsInvalidPaymentStatus() {
        Long adminUserId = 1L;
        Long paymentId = 100L;
        User admin = user(adminUserId);
        Payment payment = purchasedPayment(paymentId, user(2L));
        payment.setStatus(PaymentStatus.FAILED);

        when(userRepository.findById(adminUserId)).thenReturn(Optional.of(admin));
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
        when(entitlementRepository.findBySourcePaymentIdForUpdate(paymentId)).thenReturn(Optional.empty());

        ErrorHandler error = assertThrows(
                ErrorHandler.class,
                () -> paymentAdminRefundService.refundManually(
                        adminUserId,
                        paymentId,
                        new PaymentRequestDTO.AdminManualRefundRequest("manual refund", null)
                )
        );

        assertThat(error.getErrorCode()).isEqualTo(ErrorStatus.PAYMENT_REFUND_INVALID_STATUS);
        verify(paymentEventRepository, never()).save(any(PaymentEvent.class));
    }

    @Test
    void refundManuallyRejectsNonAdminUser() {
        Long userId = 1L;
        doThrow(new ErrorHandler(ErrorStatus.PAYMENT_ADMIN_REQUIRED))
                .when(adminAuthorizationService)
                .assertAdmin(userId);

        ErrorHandler error = assertThrows(
                ErrorHandler.class,
                () -> paymentAdminRefundService.refundManually(
                        userId,
                        100L,
                        new PaymentRequestDTO.AdminManualRefundRequest("manual refund", null)
                )
        );

        assertThat(error.getErrorCode()).isEqualTo(ErrorStatus.PAYMENT_ADMIN_REQUIRED);
        verify(paymentRepository, never()).findByIdForUpdate(any());
    }

    private Payment purchasedPayment(Long id, User user) {
        PaymentProduct product = PaymentProduct.builder()
                .id(10L)
                .productId("remove_ads")
                .platform(PaymentPlatform.GOOGLE)
                .storeProductId("premium")
                .productType(ProductType.NON_CONSUMABLE)
                .entitlementType(EntitlementType.REMOVE_ADS)
                .active(true)
                .displayName("광고 제거")
                .build();

        return Payment.builder()
                .id(id)
                .user(user)
                .platform(PaymentPlatform.GOOGLE)
                .product(product)
                .storeProductId("premium")
                .productType(ProductType.NON_CONSUMABLE)
                .status(PaymentStatus.PURCHASED)
                .googlePurchaseTokenHash("hash")
                .build();
    }

    private Entitlement activeEntitlement(User user, Payment payment) {
        return Entitlement.builder()
                .id(20L)
                .user(user)
                .entitlementType(EntitlementType.REMOVE_ADS)
                .active(true)
                .sourceType(EntitlementSourceType.PAYMENT)
                .sourcePlatform(PaymentPlatform.GOOGLE)
                .sourcePayment(payment)
                .grantedAt(LocalDateTime.of(2026, 6, 15, 10, 0))
                .build();
    }

    private User user(Long id) {
        return User.builder()
                .id(id)
                .provider("GOOGLE")
                .nickname("user")
                .build();
    }
}
