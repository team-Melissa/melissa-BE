package com.melissa.diary.repository;

import com.melissa.diary.domain.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {
    // 특정 사용자(User) ID를 통해 해당 UserSetting을 찾는다
    Optional<UserSetting> findByUserId(Long userId);
}
