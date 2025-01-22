package com.melissa.diary.converter;

import com.melissa.diary.domain.User;
import com.melissa.diary.web.dto.UserResponseDTO;
import lombok.experimental.UtilityClass;

@UtilityClass
public class UserConverter {


    public UserResponseDTO.OAuthLoginResultDTO toOAuthLoginResultDTO(
            User user,
            String accessToken,
            String refreshToken
    ) {
        return UserResponseDTO.OAuthLoginResultDTO.builder()
                .userId(user.getId())
                .oauthProvider(user.getProvider())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
    }
}