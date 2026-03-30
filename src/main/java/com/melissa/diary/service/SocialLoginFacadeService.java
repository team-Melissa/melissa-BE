package com.melissa.diary.service;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.domain.User;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.web.dto.UserRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class SocialLoginFacadeService {

    private final SocialAuthService socialAuthService;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    public User socialLoginGoogle(UserRequestDTO.GoogleOAuthDTO request) {
        SocialAuthService.GooglePayload payload = socialAuthService.verifyGoogleToken(request.getIdToken());
        if (payload == null) {
            throw new ErrorHandler(ErrorStatus.SOCIAL_LOGIN_FAILED);
        }
        return upsertSocialUser("GOOGLE", payload.getSub(), payload.getEmail(), payload.getName());
    }

    public User socialLoginKakao(UserRequestDTO.KakaoOAuthDTO request) {
        SocialAuthService.KakaoPayload payload = socialAuthService.verifyKakaoToken(request.getAccessToken());
        if (payload == null) {
            throw new ErrorHandler(ErrorStatus.SOCIAL_LOGIN_FAILED);
        }
        return upsertSocialUser("KAKAO", payload.getId(), payload.getEmail(), payload.getNickname());
    }

    public User socialLoginApple(UserRequestDTO.AppleOAuthDTO request) {
        SocialAuthService.ApplePayload payload = socialAuthService.verifyAppleToken(request.getIdToken());
        if (payload == null) {
            throw new ErrorHandler(ErrorStatus.SOCIAL_LOGIN_FAILED);
        }
        return upsertSocialUser("APPLE", payload.getSub(), payload.getEmail(), payload.getName());
    }

    private User upsertSocialUser(String provider, String providerId, String email, String nickname) {
        User user = transactionTemplate.execute(status -> {
            User socialUser = userRepository.findByProviderAndProviderId(provider, providerId)
                    .orElseGet(() -> userRepository.save(User.builder()
                            .provider(provider)
                            .providerId(providerId)
                            .email(email)
                            .nickname(nickname)
                            .build()));

            if (email != null) {
                socialUser.setEmail(email);
            }
            if (nickname != null) {
                socialUser.setNickname(nickname);
            }

            return socialUser;
        });

        if (user == null) {
            throw new IllegalStateException("Failed to upsert social user");
        }
        return user;
    }
}
