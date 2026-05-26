package com.melissa.diary.repository;

import com.melissa.diary.domain.AsyncJob;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AsyncJobRepository extends JpaRepository<AsyncJob, Long> {

    Optional<AsyncJob> findByDedupeKey(String dedupeKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT aj FROM AsyncJob aj WHERE aj.id = :id")
    Optional<AsyncJob> findByIdForUpdate(@Param("id") Long id);
}
