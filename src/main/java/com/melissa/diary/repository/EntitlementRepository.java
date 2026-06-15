package com.melissa.diary.repository;

import com.melissa.diary.domain.Entitlement;
import com.melissa.diary.domain.enums.EntitlementType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EntitlementRepository extends JpaRepository<Entitlement, Long> {

    List<Entitlement> findByUserIdAndActiveTrue(Long userId);

    Optional<Entitlement> findByUserIdAndEntitlementType(Long userId, EntitlementType entitlementType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e
            FROM Entitlement e
            WHERE e.user.id = :userId
              AND e.entitlementType = :entitlementType
            """)
    Optional<Entitlement> findByUserIdAndEntitlementTypeForUpdate(
            @Param("userId") Long userId,
            @Param("entitlementType") EntitlementType entitlementType
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e
            FROM Entitlement e
            WHERE e.sourcePayment.id = :paymentId
            """)
    Optional<Entitlement> findBySourcePaymentIdForUpdate(@Param("paymentId") Long paymentId);
}
