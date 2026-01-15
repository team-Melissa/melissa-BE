package com.melissa.diary.repository;


import com.melissa.diary.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    /**
     * Refresh 토큰 해시로 유저 찾기
     */
    Optional<User> findByRefreshToken(String refreshTokenHash);

    @Modifying
    @Query("""
    UPDATE User u
       SET u.dailyQuota = 100,
           u.quotaDate  = :today
""")
    void resetAll(@Param("today") LocalDate today);
}