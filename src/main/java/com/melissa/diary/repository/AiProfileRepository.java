package com.melissa.diary.repository;

import com.melissa.diary.domain.AiProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiProfileRepository extends JpaRepository<AiProfile, Long> {
    boolean existsByUserId(Long userId);
}
