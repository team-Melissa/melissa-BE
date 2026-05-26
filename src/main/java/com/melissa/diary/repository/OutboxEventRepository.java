package com.melissa.diary.repository;

import com.melissa.diary.domain.OutboxEvent;
import com.melissa.diary.domain.enums.OutboxEventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("""
        SELECT oe
        FROM OutboxEvent oe
        WHERE oe.status IN :statuses
          AND (oe.nextPublishAt IS NULL OR oe.nextPublishAt <= :now)
        ORDER BY oe.id ASC
        """)
    List<OutboxEvent> findPublishCandidates(
            @Param("statuses") Collection<OutboxEventStatus> statuses,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT oe FROM OutboxEvent oe WHERE oe.id = :id")
    Optional<OutboxEvent> findByIdForUpdate(@Param("id") Long id);
}
