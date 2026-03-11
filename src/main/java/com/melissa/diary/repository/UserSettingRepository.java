package com.melissa.diary.repository;

import com.melissa.diary.domain.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {
    Optional<UserSetting> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    void deleteByUserId(Long userId);

    /**
     * Notification targets:
     * 1) current slot users (notificationTime)
     * 2) retry candidates (failed today, retry count not exceeded, retry wait elapsed)
     */
    @Query("""
        SELECT DISTINCT us FROM UserSetting us
        INNER JOIN FETCH us.user u
        INNER JOIN FETCH u.expoPushTokenList ept
        WHERE us.notificationEnabled = true
        AND (us.lastSentDate IS NULL OR us.lastSentDate < :today)
        AND ept.invalid = false
        AND (
            us.notificationTime = :notificationTime
            OR (
                us.lastAttemptAt IS NOT NULL
                AND FUNCTION('DATE', us.lastAttemptAt) = :today
                AND us.retryCount < :maxRetryCount
                AND us.lastAttemptAt <= :retryEligibleBefore
            )
        )
        """)
    List<UserSetting> findNotificationTargets(
            @Param("notificationTime") Time notificationTime,
            @Param("today") LocalDate today,
            @Param("retryEligibleBefore") LocalDateTime retryEligibleBefore,
            @Param("maxRetryCount") int maxRetryCount
    );
}
