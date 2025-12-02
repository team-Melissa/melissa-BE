package com.melissa.diary.repository;

import com.melissa.diary.domain.AiProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiProfileRepository extends JpaRepository<AiProfile, Long> {
    
    Optional<AiProfile> findByIdAndActiveIsTrue(Long id);
    
    List<AiProfile> findByActiveIsTrueOrderByIdAsc();
}
