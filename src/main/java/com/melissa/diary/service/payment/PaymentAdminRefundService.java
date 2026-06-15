package com.melissa.diary.service.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.domain.Entitlement;
import com.melissa.diary.domain.Payment;
import com.melissa.diary.domain.PaymentEvent;
import com.melissa.diary.domain.User;
import com.melissa.diary.domain.enums.PaymentEventProcessingStatus;
import com.melissa.diary.domain.enums.PaymentStatus;
import com.melissa.diary.repository.EntitlementRepository;
import com.melissa.diary.repository.PaymentEventRepository;
import com.melissa.diary.repository.PaymentRepository;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.service.AdminAuthorizationService;
import com.melissa.diary.web.dto.PaymentRequestDTO;
import com.melissa.diary.web.dto.PaymentResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentAdminRefundService {

    private static final String MANUAL_REFUND_EVENT_TYPE = "MANUAL_REFUND";

    private final AdminAuthorizationService adminAuthorizationService;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final EntitlementRepository entitlementRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public PaymentResponseDTO.AdminManualRefundResponse refundManually(
            Long adminUserId,
            Long paymentId,
            PaymentRequestDTO.AdminManualRefundRequest request
    ) {
        adminAuthorizationService.assertAdmin(adminUserId);

        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.PAYMENT_NOT_FOUND));
        Entitlement entitlement = entitlementRepository
                .findBySourcePaymentIdForUpdate(paymentId)
                .orElse(null);

        LocalDateTime processedAt = LocalDateTime.now();
        boolean alreadyProcessed = isAlreadyRefunded(payment);
        if (alreadyProcessed) {
            if (entitlement != null && Boolean.TRUE.equals(entitlement.getActive())) {
                LocalDateTime revokedAt = firstNonNull(payment.getRefundedAt(), payment.getRevokedAt(), processedAt);
                revokeEntitlementIfOwnedByPayment(entitlement, request.getReason(), revokedAt);
                saveManualRefundEvent(payment, adminUser, request.getReason(), revokedAt, processedAt);
            }
            return toResponse(payment, entitlement, true, processedAt);
        }

        if (payment.getStatus() != PaymentStatus.PURCHASED) {
            throw new ErrorHandler(ErrorStatus.PAYMENT_REFUND_INVALID_STATUS);
        }

        LocalDateTime refundedAt = request.getRefundedAt() == null ? processedAt : request.getRefundedAt();
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(refundedAt);
        payment.setFailureReason(null);

        revokeEntitlementIfOwnedByPayment(entitlement, request.getReason(), refundedAt);
        saveManualRefundEvent(payment, adminUser, request.getReason(), refundedAt, processedAt);

        return toResponse(payment, entitlement, false, processedAt);
    }

    private boolean isAlreadyRefunded(Payment payment) {
        return payment.getStatus() == PaymentStatus.REFUNDED || payment.getStatus() == PaymentStatus.REVOKED;
    }

    private void revokeEntitlementIfOwnedByPayment(
            Entitlement entitlement,
            String reason,
            LocalDateTime revokedAt
    ) {
        if (entitlement == null || !Boolean.TRUE.equals(entitlement.getActive())) {
            return;
        }

        entitlement.setActive(false);
        entitlement.setRevokedAt(revokedAt);
        entitlement.setRevocationReason(truncate(reason, 100));
    }

    private void saveManualRefundEvent(
            Payment payment,
            User adminUser,
            String reason,
            LocalDateTime refundedAt,
            LocalDateTime processedAt
    ) {
        String eventId = manualRefundEventId(payment.getId());
        if (paymentEventRepository.findByPlatformAndEventId(payment.getPlatform(), eventId).isPresent()) {
            return;
        }

        PaymentEvent event = PaymentEvent.builder()
                .platform(payment.getPlatform())
                .eventId(eventId)
                .eventType(MANUAL_REFUND_EVENT_TYPE)
                .payment(payment)
                .actorUser(adminUser)
                .googlePurchaseTokenHash(payment.getGooglePurchaseTokenHash())
                .appleTransactionId(payment.getAppleTransactionId())
                .rawPayload(rawPayload(payment, reason, refundedAt))
                .receivedAt(processedAt)
                .processedAt(processedAt)
                .processingStatus(PaymentEventProcessingStatus.PROCESSED)
                .build();

        paymentEventRepository.save(event);
    }

    private String rawPayload(Payment payment, String reason, LocalDateTime refundedAt) {
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("source", MANUAL_REFUND_EVENT_TYPE);
            payload.put("paymentId", payment.getId());
            payload.put("reason", reason);
            payload.put("refundedAt", refundedAt.toString());
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new ErrorHandler(ErrorStatus.PAYMENT_GRANT_FAILED);
        }
    }

    private String manualRefundEventId(Long paymentId) {
        return MANUAL_REFUND_EVENT_TYPE + ":" + paymentId;
    }

    private LocalDateTime firstNonNull(LocalDateTime first, LocalDateTime second, LocalDateTime fallback) {
        if (first != null) {
            return first;
        }
        return second == null ? fallback : second;
    }

    private PaymentResponseDTO.AdminManualRefundResponse toResponse(
            Payment payment,
            Entitlement entitlement,
            boolean alreadyProcessed,
            LocalDateTime processedAt
    ) {
        return PaymentResponseDTO.AdminManualRefundResponse.builder()
                .paymentId(payment.getId())
                .userId(payment.getUser().getId())
                .platform(payment.getPlatform().name())
                .productId(payment.getProduct().getProductId())
                .status(payment.getStatus().name())
                .entitlementType(entitlement == null ? null : entitlement.getEntitlementType().name())
                .entitlementRevoked(entitlement != null && !Boolean.TRUE.equals(entitlement.getActive()))
                .entitlementActive(entitlement == null ? null : Boolean.TRUE.equals(entitlement.getActive()))
                .alreadyProcessed(alreadyProcessed)
                .refundedAt(payment.getRefundedAt())
                .processedAt(processedAt)
                .build();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
