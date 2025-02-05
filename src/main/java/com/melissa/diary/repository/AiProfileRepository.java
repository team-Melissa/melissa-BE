package com.melissa.diary.repository;

import com.melissa.diary.domain.AiProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiProfileRepository extends JpaRepository<AiProfile, Long> {
    List<AiProfile> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    void deleteAllByUserId(Long userId);
}
