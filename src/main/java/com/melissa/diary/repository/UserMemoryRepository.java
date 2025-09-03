package com.melissa.diary.repository;

import com.melissa.diary.domain.UserMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserMemoryRepository extends JpaRepository<UserMemory, Long> {
    
    /**
     * 사용자 ID로 메모리 조회
     */
    Optional<UserMemory> findByUserId(Long userId);
    
    /**
     * 사용자 ID로 메모리 존재 여부 확인
     */
    boolean existsByUserId(Long userId);
    
    /**
     * 사용자 ID로 메모리 삭제
     */
    void deleteByUserId(Long userId);
    
    /**
     * 메모리 내용이 있는 사용자 수 조회 (통계용)
     */
    @Query("SELECT COUNT(um) FROM UserMemory um WHERE um.memoryContent IS NOT NULL AND um.memoryContent != ''")
    long countUsersWithMemory();
    
    /**
     * 특정 기간 이후 업데이트된 메모리 개수 조회
     */
    @Query("SELECT COUNT(um) FROM UserMemory um WHERE um.lastUpdatedAt >= :since")
    long countRecentlyUpdatedMemories(@Param("since") java.time.LocalDateTime since);
}
