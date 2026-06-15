package com.melissa.diary.repository;

import com.melissa.diary.domain.Payment;
import com.melissa.diary.domain.enums.PaymentPlatform;
import com.melissa.diary.domain.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM Payment p
            WHERE p.platform = :platform
              AND p.googlePurchaseTokenHash = :tokenHash
            """)
    Optional<Payment> findByPlatformAndGooglePurchaseTokenHashForUpdate(
            @Param("platform") PaymentPlatform platform,
            @Param("tokenHash") String tokenHash
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM Payment p
            WHERE p.platform = :platform
              AND p.appleTransactionId = :transactionId
            """)
    Optional<Payment> findByPlatformAndAppleTransactionIdForUpdate(
            @Param("platform") PaymentPlatform platform,
            @Param("transactionId") String transactionId
    );

    @EntityGraph(attributePaths = "product")
    List<Payment> findTop20ByPlatformAndStatusAndAcknowledgedAtIsNullAndGooglePurchaseTokenEncryptedIsNotNullOrderByCreatedAtAsc(
            PaymentPlatform platform,
            PaymentStatus status
    );
}
