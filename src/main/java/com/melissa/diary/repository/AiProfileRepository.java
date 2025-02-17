package com.melissa.diary.repository;

import com.melissa.diary.domain.AiProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiProfileRepository extends JpaRepository<AiProfile, Long> {
    List<AiProfile> findByUserId(Long userId);

    // active = true 인 것만 조회 -> 목록 조회
    List<AiProfile> findByUserIdAndActiveIsTrue(Long userId);

    // ID, active 둘 다 만족하는 프로필 조회 -> 단건 조회
    Optional<AiProfile> findByIdAndActiveIsTrue(Long id);


    boolean existsByUserId(Long userId);
    void deleteAllByUserId(Long userId);
}
