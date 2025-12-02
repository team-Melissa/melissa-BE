package com.melissa.diary.repository;

import com.melissa.diary.domain.DailyChatLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DailyChatLogRepository extends JpaRepository<DailyChatLog, Long> {
    
    Optional<DailyChatLog> findFirstByThreadUserIdOrderByCreatedAtDesc(Long userId);
}
