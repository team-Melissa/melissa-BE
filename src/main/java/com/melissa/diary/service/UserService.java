package com.melissa.diary.service;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.domain.User;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.security.JwtProvider;
import com.melissa.diary.web.dto.UserRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SocialAuthService socialAuthService;
    private final JwtProvider jwtProvider;

    @Transactional
    public User socialLoginGoogle(UserRequestDTO.GoogleOAuthDto request) {
        // 1) 구글 ID Token 검증
        SocialAuthService.GooglePayload payload =
                socialAuthService.verifyGoogleToken(request.getIdToken());

        if (payload == null) {
            throw new ErrorHandler(ErrorStatus.SOCIAL_LOGIN_FAILED);
        }

        // 2) DB 조회 or 가입
        User user = userRepository.findByProviderAndProviderId("GOOGLE", payload.getSub())
                .orElseGet(() -> {
                    User newU = User.builder()
                            .provider("GOOGLE")
                            .providerId(payload.getSub())
                            .email(payload.getEmail())
                            .nickname(payload.getName())
                            .build();
                    return userRepository.save(newU);
                });

        // 기존 유저 정보 갱신
        user.setEmail(payload.getEmail());
        user.setNickname(payload.getName());

        return user;
    }



    @Transactional
    public User socialLoginKakao(UserRequestDTO.KakaoOAuthDto request) {
        SocialAuthService.KakaoPayload payload =
            socialAuthService.verifyKakaoToken(request.getAccessToken());

        if (payload == null) {
            throw new ErrorHandler(ErrorStatus.SOCIAL_LOGIN_FAILED);
        }
        // DB 조회 or 가입
        User user = userRepository.findByProviderAndProviderId("KAKAO", payload.getId())
                .orElseGet(() -> {
                    User newU = User.builder()
                            .provider("KAKAO")
                            .providerId(payload.getId())
                            .email(payload.getEmail())
                            .nickname(payload.getNickname())
                            .build();
                    return userRepository.save(newU);
                });
        user.setEmail(payload.getEmail());
        user.setNickname(payload.getNickname());

        return user;
    }



    /*
    @Transactional
    public User socialLoginApple(UserRequestDTO.AppleOAuthDto request) {
        SocialAuthService.ApplePayload payload =
            socialAuthService.verifyAppleToken(request.getIdToken());

        if (payload == null) {
            throw new ErrorHandler(ErrorStatus.SOCIAL_LOGIN_FAILED);
        }
        User user = userRepository.findByOauthProviderAndOauthProviderId("APPLE", payload.getSub())
                .orElseGet(() -> {
                    User newU = User.builder()
                            .oauthProvider("APPLE")
                            .oauthProviderId(payload.getSub())
                            .email(payload.getEmail())
                            .nickname(payload.getName())
                            .build();
                    return userRepository.save(newU);
                });
        if (payload.getEmail() != null) user.setEmail(payload.getEmail());
        if (payload.getName() != null) user.setNickname(payload.getName());
        return user;
    }
    */

    /**
     * [AccessToken, RefreshToken] 발급
     */
    @Transactional
    public String createAccessToken(User user) {
        return jwtProvider.createAccessToken(user.getId(), user.getProvider());
    }

    @Transactional
    public String createRefreshToken(User user) {
        String token = jwtProvider.createRefreshToken(user.getId(), user.getProvider());
        user.setRefreshToken(token);
        user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(1));
        return token;
    }

    /**
     * [5] Refresh Token으로 Access Token 재발급
     */
    @Transactional
    public User refreshAccessToken(String refreshToken) {
        // DB에서 refreshToken으로 유저 찾기
        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh Token이 유효하지 않습니다."));

        // 토큰 만료 시각 확인
        if (user.getRefreshTokenExpiry() == null ||
                user.getRefreshTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh Token이 만료되었습니다. 재로그인 필요.");
        }

        // Refresh 토큰도 JWT라면 validate
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Refresh Token이 만료 또는 위조되었습니다.");
        }

        // Access Token 재발급
        return user;
    }
}
