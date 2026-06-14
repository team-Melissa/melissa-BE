package com.melissa.diary.repository;

import com.melissa.diary.domain.Entitlement;
import com.melissa.diary.domain.enums.EntitlementType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EntitlementRepository extends JpaRepository<Entitlement, Long> {

    List<Entitlement> findByUserIdAndActiveTrue(Long userId);

    Optional<Entitlement> findByUserIdAndEntitlementType(Long userId, EntitlementType entitlementType);
}
