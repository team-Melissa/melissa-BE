package com.melissa.diary.repository;

import com.melissa.diary.domain.DailyChatLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyChatLogRepository extends JpaRepository<DailyChatLog, Long> {
}
