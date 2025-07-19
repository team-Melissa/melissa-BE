package com.melissa.diary.repository;

import com.melissa.diary.domain.AiProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // 정렬: 최근 사용 시각(last_used_at) → 생성일 순
    @Query(value = """
            SELECT p.*
            FROM ai_profile p
            WHERE p.user_id = :userId
              AND p.active  = true
            ORDER BY p.last_used_at IS NULL,
                     p.last_used_at DESC,
                     p.created_at DESC
            """, nativeQuery = true)
    List<AiProfile> findActiveProfilesOrderByLastUsed(@Param("userId") Long userId);
}
