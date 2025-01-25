package com.melissa.diary.web.dto;

import lombok.Builder;
import lombok.Getter;

public class UserResponseDTO {
    @Getter
    @Builder
    public static class OAuthLoginResultDTO {
        private Long userId;
        private String oauthProvider; // "GOOGLE", "KAKAO", etc
        private String email;
        private String nickname;

        private String accessToken;
        private String refreshToken;
        private String tokenType;
    }

    @Getter
    @Builder
    public static class RefreshTokenResponseDTO{
        private String refreshToken;
        private String tokenType;
        private int expireIn;

    }
}
