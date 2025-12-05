package com.melissa.diary.repository;

import com.melissa.diary.domain.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Time;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {
    // 특정 사용자(User) ID를 통해 해당 UserSetting을 찾는다
    Optional<UserSetting> findByUserId(Long userId);
    boolean existsByUserId(Long userId);

    void deleteByUserId(Long userId);

    /**
     * 알림 발송 대상 조회 (시간 일치, 알림 활성화, 미발송, 유효 토큰)
     */
    @Query("""
        SELECT DISTINCT us FROM UserSetting us
        INNER JOIN us.user u
        INNER JOIN u.expoPushTokenList ept
        WHERE us.notificationTime = :notificationTime
        AND (us.notificationSummary = true OR us.notificationQna = true)
        AND (us.lastSentDate IS NULL OR us.lastSentDate < :today)
        AND ept.invalid = false
        """)
    List<UserSetting> findNotificationTargets(
            @Param("notificationTime") Time notificationTime,
            @Param("today") LocalDate today
    );

}
