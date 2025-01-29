package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.converter.UserConverter;
import com.melissa.diary.domain.User;
import com.melissa.diary.service.UserService;
import com.melissa.diary.web.dto.UserRequestDTO;
import com.melissa.diary.web.dto.UserResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@Tag(name = "AuthAPI", description = "Auth 관련 API")
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    // 구글 로그인
    @PostMapping("/google")
    @Operation(description = "구글로그인으로, id토큰을 입력해주세요")
    public ApiResponse<UserResponseDTO.OAuthLoginResultDTO> googleLogin(
            @RequestBody @Valid UserRequestDTO.GoogleOAuthDTO request
    ) {
        // 1) 소셜 로그인 처리
        User user = userService.socialLoginGoogle(request);

        // 2) JWT 발급
        String accessToken = userService.createAccessToken(user);
        String refreshToken = userService.createRefreshToken(user);

        // 3) DTO 변환
        UserResponseDTO.OAuthLoginResultDTO result = UserConverter
                .toOAuthLoginResultDTO(user, accessToken, refreshToken);

        return ApiResponse.onSuccess(result);
    }

    // 카카오 로그인
    @PostMapping("/kakao")
    @Operation(description = "카카오로그인으로, 액세스토큰을 입력해주세요")
    public ApiResponse<UserResponseDTO.OAuthLoginResultDTO> kakaoLogin(
            @RequestBody @Valid UserRequestDTO.KakaoOAuthDTO request
    ) {
        User user = userService.socialLoginKakao(request);
        String accessToken = userService.createAccessToken(user);
        String refreshToken = userService.createRefreshToken(user);
        UserResponseDTO.OAuthLoginResultDTO result =
                UserConverter.toOAuthLoginResultDTO(user, accessToken, refreshToken);
        return ApiResponse.onSuccess(result);
    }

    /*@PostMapping("/oauth/apple")
    public ApiResponse<UserResponseDTO.OAuthLoginResultDTO> appleLogin(
            @RequestBody @Valid UserRequestDTO.AppleOAuthDto request
    ) {
        User user = userService.socialLoginApple(request);
        String accessToken = userService.createAccessToken(user);
        String refreshToken = userService.createRefreshToken(user);
        UserResponseDTO.OAuthLoginResultDTO result =
                UserConverter.toOAuthLoginResultDTO(user, accessToken, refreshToken);
        return ApiResponse.onSuccess(result);
    }*/


    // Refresh Token 재발급
    // Refresh Token 재발급
    @PostMapping("/refresh")
    @Operation(description = "Refresh토큰을 입력해, AccessToken 재생성합니다.")
    public ApiResponse<UserResponseDTO.OAuthLoginResultDTO> refreshToken(
            @RequestBody @Valid UserRequestDTO.RefreshRequestDTO request
    ) {
        // [1] refreshToken으로 사용자 조회 & 검증
        User user = userService.refreshAccessToken(request.getRefreshToken());

        // [2] 새로운 Token 생성
        String newAccessToken = userService.createAccessToken(user);
        String newRefreshToken = userService.createRefreshToken(user);



        // [3] 결과 DTO 생성
        UserResponseDTO.OAuthLoginResultDTO result =
                UserResponseDTO.OAuthLoginResultDTO.builder()
                        .userId(user.getId())
                        .oauthProvider(user.getProvider())
                        .email(user.getEmail())
                        .nickname(user.getNickname())
                        .accessToken(newAccessToken)
                        .refreshToken(newRefreshToken) // 새로 갱신한 토큰
                        .tokenType("Bearer")
                        .build();

        return ApiResponse.onSuccess(result);
    }

    // 로그아웃
    @PostMapping("/logout")
    @Operation(description = "로그아웃 기능으로, 서버의 토큰을 지웁니다.")
    public ApiResponse<Void> logout(Principal principal) {

        userService.logout(Long.parseLong(principal.getName()));

        return ApiResponse.onSuccess(null);
    }

}
