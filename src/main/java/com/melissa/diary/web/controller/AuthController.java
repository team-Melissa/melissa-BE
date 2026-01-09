package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.converter.UserConverter;
import com.melissa.diary.domain.User;
import com.melissa.diary.service.UserService;
import com.melissa.diary.web.dto.UserRequestDTO;
import com.melissa.diary.web.dto.UserResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    @Operation(
            summary = "구글 로그인",
            description = "구글 idToken을 검증하고, Access/Refresh 토큰을 발급합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "COMMON400: 잘못된 요청(요청 바디 검증 실패 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH4001: 소셜 로그인 인증 실패")
    })
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
    @Operation(
            summary = "카카오 로그인",
            description = "카카오 accessToken을 검증하고, Access/Refresh 토큰을 발급합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "COMMON400: 잘못된 요청(요청 바디 검증 실패 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH4001: 소셜 로그인 인증 실패")
    })
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

    // --- Apple 로그인 ---
    @PostMapping("/apple")
    @Operation(
            summary = "애플 로그인",
            description = "애플 idToken을 검증하고, Access/Refresh 토큰을 발급합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "COMMON400: 잘못된 요청(요청 바디 검증 실패 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH4001: 소셜 로그인 인증 실패")
    })
    public ApiResponse<UserResponseDTO.OAuthLoginResultDTO> appleLogin(
            @RequestBody @Valid UserRequestDTO.AppleOAuthDTO request
    ) {
        User user = userService.socialLoginApple(request);
        String accessToken = userService.createAccessToken(user);
        String refreshToken = userService.createRefreshToken(user);
        UserResponseDTO.OAuthLoginResultDTO result =
                UserConverter.toOAuthLoginResultDTO(user, accessToken, refreshToken);
        return ApiResponse.onSuccess(result);
    }

    // Refresh Token 재발급
    @PostMapping("/refresh")
    @Operation(
            summary = "토큰 재발급(Refresh)",
            description = """
                Authorization 헤더에 Refresh Token(Bearer)을 전달하여 Access/Refresh 토큰을 재발급합니다.
                - 예: Authorization: Bearer {refreshToken}
                """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH4002/AUTH4003/AUTH4005: 토큰이 유효하지 않음/만료/검증 실패")
    })
    public ApiResponse<UserResponseDTO.OAuthLoginResultDTO> refreshToken(
            HttpServletRequest request
    ) {
        // [1] Authorization 헤더에서 refreshToken 추출
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ErrorHandler(ErrorStatus.INVALID_TOKEN);
        }
        String refreshToken = authHeader.substring(7);

        // [2] refreshToken으로 사용자 조회 & 검증
        User user = userService.refreshAccessToken(refreshToken);

        // [3] 새로운 Token 생성
        String newAccessToken = userService.createAccessToken(user);
        String newRefreshToken = userService.createRefreshToken(user);

        // [4] 결과 DTO 생성
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
    @Operation(
            summary = "로그아웃",
            description = "현재 로그인 사용자의 서버 저장 Refresh Token을 제거합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH4002/AUTH4003/AUTH4005: 토큰이 유효하지 않음/만료/검증 실패")
    })
    public ApiResponse<Void> logout(Principal principal) {

        userService.logout(Long.parseLong(principal.getName()));

        return ApiResponse.onSuccess(null);
    }

}
