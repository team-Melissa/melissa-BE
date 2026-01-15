package com.melissa.diary.service;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.converter.UserConverter;
import com.melissa.diary.domain.User;
import com.melissa.diary.repository.ThreadRepository;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.repository.UserSettingRepository;
import com.melissa.diary.security.JwtProvider;
import com.melissa.diary.security.JwtTokenType;
import com.melissa.diary.security.RefreshTokenHasher;
import com.melissa.diary.security.TokenValidationResult;
import com.melissa.diary.web.dto.UserRequestDTO;
import com.melissa.diary.web.dto.UserResponseDTO;
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
    private final RefreshTokenHasher refreshTokenHasher;
    private final ThreadRepository threadRepository;
    private final UserSettingRepository userSettingRepository;


    @Transactional
    public User socialLoginGoogle(UserRequestDTO.GoogleOAuthDTO request) {
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
    public User socialLoginKakao(UserRequestDTO.KakaoOAuthDTO request) {
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




    @Transactional
    public User socialLoginApple(UserRequestDTO.AppleOAuthDTO request) {
        SocialAuthService.ApplePayload payload =
            socialAuthService.verifyAppleToken(request.getIdToken());

        if (payload == null) {
            throw new ErrorHandler(ErrorStatus.SOCIAL_LOGIN_FAILED);
        }
        User user = userRepository.findByProviderAndProviderId("APPLE", payload.getSub())
                .orElseGet(() -> {
                    User newU = User.builder()
                            .provider("APPLE")
                            .providerId(payload.getSub())
                            .email(payload.getEmail())
                            .nickname(payload.getName())
                            .build();
                    return userRepository.save(newU);
                });
        if (payload.getEmail() != null) user.setEmail(payload.getEmail());
        if (payload.getName() != null) user.setNickname(payload.getName());
        return user;
    }


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
        // refreshToken 컬럼에 해시값 저장
        user.setRefreshToken(refreshTokenHasher.sha256Hex(token));
        user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(15));
        userRepository.save(user);
        return token;
    }

    @Transactional
    public User refreshAccessToken(String refreshToken) {
        // Refresh 토큰(JWT) 자체의 유효성(만료/위조 등) 먼저 검증
        // - 타입(typ) 파싱 과정에서 예외가 발생할 수 있으므로, 순서를 앞당겨 500을 방지
        TokenValidationResult validation = jwtProvider.validateTokenResult(refreshToken);
        if (validation == TokenValidationResult.EXPIRED) {
            throw new ErrorHandler(ErrorStatus.EXPIRED_TOKEN);
        }
        if (validation == TokenValidationResult.INVALID) {
            throw new ErrorHandler(ErrorStatus.TOKEN_VERIFICATION_FAILED);
        }

        // DB에서 refreshToken 해시로 유저 찾기
        String refreshTokenHash = refreshTokenHasher.sha256Hex(refreshToken);
        User user = userRepository.findByRefreshToken(refreshTokenHash)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.INVALID_TOKEN));
        // -> "Refresh Token이 유효하지 않습니다." 의미로 INVALID_TOKEN 사용

        // 토큰 만료 시각 확인
        if (user.getRefreshTokenExpiry() == null
                || user.getRefreshTokenExpiry().isBefore(LocalDateTime.now())) {
            // -> Refresh Token이 만료된 경우
            throw new ErrorHandler(ErrorStatus.EXPIRED_TOKEN);
        }

        // Refresh 토큰 타입 검증 (access 토큰으로 refresh 호출 방지)
        JwtTokenType tokenType = jwtProvider.getTokenTypeOrNull(refreshToken);
        if (tokenType != JwtTokenType.REFRESH) {
            throw new ErrorHandler(ErrorStatus.TOKEN_TYPE_MISMATCH);
        }
        // [4] Access Token 재발급시 필요한 사용자 반환
        return user;
    }

    @Transactional
    public void logout(Long userId) {
        // DB에서 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.INVALID_TOKEN));

        // Refresh Token 해시 삭제
        user.setRefreshToken(null);
        user.setRefreshTokenExpiry(null);

    }

    @Transactional
    public UserResponseDTO.DeleteResultDTO deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));

        // 유저의 스레드 및 관련 채팅 내역 삭제
        threadRepository.deleteAllByUserId(userId);

        // 유저 설정 삭제
        userSettingRepository.deleteByUserId(userId);

        // 삭제될 유저 정보를 DTO로 변환
        UserResponseDTO.DeleteResultDTO deleteDTO = UserConverter.toDeleteDTO(user);

        // 유저 계정 삭제
        userRepository.delete(user);

        return deleteDTO;
    }

    @Transactional(readOnly = true)
    public UserResponseDTO.MeResponseDTO getMe(Long userId){
        // 유저 정보 조회
        User user = userRepository.findById(userId).orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));

        return UserConverter.toGetMe(user);
    }

}
