package com.melissa.diary.repository;

import com.melissa.diary.domain.DefaultAiProfile;
import com.melissa.diary.domain.UserDefaultAiProfileMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserDefaultAiProfileMappingRepository extends JpaRepository<UserDefaultAiProfileMapping, Long> {
    @Query("""
        select p
        from UserDefaultAiProfileMapping m
        join m.defaultAiProfile p
        where m.user.id = :userId
          and m.active = true
    """)
    List<DefaultAiProfile> findActiveProfilesByUserId(@Param("userId") Long userId);

    @Query("""
        select p
        from UserDefaultAiProfileMapping m
        join m.defaultAiProfile p
        where m.user.id = :userId
          and p.id       = :profileId
          and m.active   = true
    """)
    Optional<DefaultAiProfile> findActiveProfile(@Param("userId") Long userId, @Param("profileId") Long profileId);

    @Query("""
        select m.defaultAiProfile.id
        from UserDefaultAiProfileMapping m
        where m.user.id = :userId
    """)
    List<Long> findProfileIdsByUserId(@Param("userId") Long userId);

    List<UserDefaultAiProfileMapping> findByUserId(Long userId);

    @Query("""
        select m
        from UserDefaultAiProfileMapping m
        join fetch m.defaultAiProfile p
        where m.user.id = :userId
          and m.active = true
    """)
    List<UserDefaultAiProfileMapping> findActiveMappingsWithProfileByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndDefaultAiProfileId(Long userId, Long defaultAiProfileId);
} 