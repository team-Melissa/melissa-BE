package com.melissa.diary.repository;

import com.melissa.diary.domain.PaymentProduct;
import com.melissa.diary.domain.enums.PaymentPlatform;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentProductRepository extends JpaRepository<PaymentProduct, Long> {

    Optional<PaymentProduct> findByPlatformAndStoreProductIdAndActiveTrue(
            PaymentPlatform platform,
            String storeProductId
    );
}
