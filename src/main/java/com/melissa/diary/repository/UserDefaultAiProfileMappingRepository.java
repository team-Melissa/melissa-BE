package com.melissa.diary.repository;

import com.melissa.diary.domain.UserDefaultAiProfileMapping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDefaultAiProfileMappingRepository extends JpaRepository<UserDefaultAiProfileMapping, Long> {
    java.util.Optional<UserDefaultAiProfileMapping> findByUserIdAndDefaultAiProfileId(Long userId, Long defaultAiProfileId);
} 