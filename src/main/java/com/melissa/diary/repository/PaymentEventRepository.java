package com.melissa.diary.repository;

import com.melissa.diary.domain.PaymentEvent;
import com.melissa.diary.domain.enums.PaymentPlatform;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {

    Optional<PaymentEvent> findByPlatformAndEventId(PaymentPlatform platform, String eventId);
}
