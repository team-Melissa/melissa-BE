package com.melissa.diary.repository;


import com.melissa.diary.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    // Refresh 토큰으로 유저 찾기 (간단 예시 - 실제론 해시 등 보안처리)
    Optional<User> findByRefreshToken(String refreshToken);


}