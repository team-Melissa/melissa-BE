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

    // 가장 최근에 생성된 AiProfile (active 상태) 하나 조회
    Optional<AiProfile> findFirstByUserIdAndActiveIsTrueOrderByCreatedAtDesc(Long userId);

    boolean existsByUserId(Long userId);
    boolean existsByUserIdAndDefaultId(Long userId, Long defaultId);
    boolean existsByUserIdAndDefaultIdAndActiveIsTrue(Long userId, Long defaultId);

    // 사용자 소유의 기본 제공 프로필 (active 여부 무관)
    List<AiProfile> findByUserIdAndDefaultIdIsNotNull(Long userId);
    void deleteAllByUserId(Long userId);
}
