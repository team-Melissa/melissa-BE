package com.melissa.diary.repository;

import com.melissa.diary.domain.IdempotencyRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {

    @Modifying
    @Query(value = """
        INSERT IGNORE INTO idempotency_record (
            user_id,
            endpoint,
            idempotency_key,
            request_hash,
            status,
            expires_at,
            created_at,
            updated_at,
            version
        )
        VALUES (
            :userId,
            :endpoint,
            :idempotencyKey,
            :requestHash,
            'PENDING',
            :expiresAt,
            NOW(),
            NOW(),
            0
        )
        """, nativeQuery = true)
    int insertPendingIfAbsent(
            @Param("userId") Long userId,
            @Param("endpoint") String endpoint,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("requestHash") String requestHash,
            @Param("expiresAt") LocalDateTime expiresAt
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT ir
        FROM IdempotencyRecord ir
        WHERE ir.userId = :userId
          AND ir.endpoint = :endpoint
          AND ir.idempotencyKey = :idempotencyKey
        """)
    Optional<IdempotencyRecord> findForUpdate(
            @Param("userId") Long userId,
            @Param("endpoint") String endpoint,
            @Param("idempotencyKey") String idempotencyKey
    );

    long deleteByExpiresAtBefore(LocalDateTime cutoff);
}
