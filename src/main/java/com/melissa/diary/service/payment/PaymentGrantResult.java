package com.melissa.diary.service.payment;

import com.melissa.diary.domain.Entitlement;
import com.melissa.diary.domain.Payment;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentGrantResult {
    private Payment payment;
    private Entitlement entitlement;
    private boolean alreadyProcessed;
}
