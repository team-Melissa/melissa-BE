package com.melissa.diary.repository;

import com.melissa.diary.domain.ExpoPushToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpoPushTokenRepository extends JpaRepository<ExpoPushToken, Long> {
    
    /**
     * Expo Push Token으로 조회
     */
    Optional<ExpoPushToken> findByExpoPushToken(String expoPushToken);
    
    /**
     * 사용자 ID로 토큰 목록 조회
     */
    List<ExpoPushToken> findByUserId(Long userId);
    
    /**
     * 모든 유효한 토큰 조회 (배치 발송용)
     */
    List<ExpoPushToken> findByInvalidFalse();
}

