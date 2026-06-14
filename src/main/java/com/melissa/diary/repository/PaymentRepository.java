package com.melissa.diary.repository;

import com.melissa.diary.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByUserId(Long userId);
}
