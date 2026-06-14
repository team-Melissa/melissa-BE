package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.service.EntitlementService;
import com.melissa.diary.service.UserService;
import com.melissa.diary.web.dto.EntitlementResponseDTO;
import com.melissa.diary.web.dto.UserResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@Tag(name = "UserAPI", description = "유저 관련 API")
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final EntitlementService entitlementService;

    @Operation(description = "회원탈퇴하고, 유저 정보를 리턴합니다.")
    @DeleteMapping
    public ApiResponse<UserResponseDTO.DeleteResultDTO> deleteUser(Principal principal) {
        Long userId = Long.parseLong(principal.getName());

        UserResponseDTO.DeleteResultDTO response = userService.deleteUser(userId);

        return ApiResponse.onSuccess(response);
    }

    @Operation(
            summary = "내 유저 정보 조회",
            description = """
                현재 로그인한 사용자의 기본 정보(이메일/닉네임/OAuth 제공자)와 사용량(쿼터) 정보를 반환합니다.
                - 인증 필요 (Bearer JWT)
                - userId는 토큰(subject) 기반으로 서버에서 결정
                """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패(토큰 누락/만료/위조)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "AUTH4006: 사용자를 찾을 수 없음")
    })
    @GetMapping("/me")
    public ApiResponse<UserResponseDTO.MeResponseDTO> getUser(Principal principal){
        Long userId = Long.parseLong(principal.getName());

        UserResponseDTO.MeResponseDTO response = userService.getMe(userId);

        return ApiResponse.onSuccess(response);
    }

    @Operation(
            summary = "Get current user's payment entitlements",
            description = "Returns active payment entitlements and derived feature flags for the authenticated user."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "AUTH4006: User not found")
    })
    @GetMapping("/me/entitlements")
    public ApiResponse<EntitlementResponseDTO.EntitlementsResponse> getMyEntitlements(Principal principal) {
        Long userId = Long.parseLong(principal.getName());

        EntitlementResponseDTO.EntitlementsResponse response = entitlementService.getUserEntitlements(userId);

        return ApiResponse.onSuccess(response);
    }
}
