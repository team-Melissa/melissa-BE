package com.melissa.diary.service.payment;

import com.melissa.diary.config.PaymentProperties;
import com.melissa.diary.domain.Payment;
import com.melissa.diary.domain.enums.PaymentPlatform;
import com.melissa.diary.domain.enums.PaymentStatus;
import com.melissa.diary.repository.PaymentRepository;
import com.melissa.diary.security.EncryptionManager;
import com.melissa.diary.service.payment.store.GoogleAcknowledgeCommand;
import com.melissa.diary.service.payment.store.GooglePlayPurchaseVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAcknowledgeRetryScheduler {

    private final PaymentProperties paymentProperties;
    private final PaymentRepository paymentRepository;
    private final EncryptionManager encryptionManager;
    private final GooglePlayPurchaseVerifier googlePlayPurchaseVerifier;
    private final PaymentGrantService paymentGrantService;

    @Scheduled(fixedDelayString = "${payment.google.acknowledge-retry-fixed-delay-ms:600000}")
    public void retryPendingAcknowledgements() {
        if (!paymentProperties.getIap().isEnabled() || !paymentProperties.getGoogle().isEnabled()) {
            return;
        }

        List<Payment> pendingPayments = paymentRepository
                .findTop20ByPlatformAndStatusAndAcknowledgedAtIsNullAndGooglePurchaseTokenEncryptedIsNotNullOrderByCreatedAtAsc(
                        PaymentPlatform.GOOGLE,
                        PaymentStatus.PURCHASED
                )
                .stream()
                .limit(paymentProperties.getGoogle().getAcknowledgeRetryBatchSize())
                .toList();

        for (Payment payment : pendingPayments) {
            retryOne(payment);
        }
    }

    private void retryOne(Payment payment) {
        try {
            String purchaseToken = encryptionManager.decrypt(payment.getGooglePurchaseTokenEncrypted());
            googlePlayPurchaseVerifier.acknowledge(new GoogleAcknowledgeCommand(
                    paymentProperties.getGoogle().getPackageName(),
                    payment.getProduct().getStoreProductId(),
                    purchaseToken
            ));
            paymentGrantService.markGoogleAcknowledged(payment.getId());
        } catch (RuntimeException e) {
            log.warn("[GoogleAcknowledgeRetryScheduler] retry failed. paymentId={}", payment.getId(), e);
            paymentGrantService.markGoogleAcknowledgeFailed(payment.getId(), e);
        }
    }
}
