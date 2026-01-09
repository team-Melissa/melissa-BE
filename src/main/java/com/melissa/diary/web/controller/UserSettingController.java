package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.service.UserSettingService;
import com.melissa.diary.web.dto.UserSettingRequestDTO;
import com.melissa.diary.web.dto.UserSettingResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;


@RestController
@Tag(name = "UserSettingAPI", description = "유저 셋팅관련 API")
@RequestMapping("/api/v1/user-settings")
@RequiredArgsConstructor
public class UserSettingController {

    private final UserSettingService userSettingService;
    @Operation(
            summary = "유저 설정 조회",
            description = "현재 로그인한 사용자의 설정 정보를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패(토큰 누락/만료/위조)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "AUTH4006: 유저 없음 / SETTING4001: 설정 없음")
    })
    @GetMapping
    public ApiResponse<UserSettingResponseDTO.UserSettingResponse> getUserSetting(Principal principal) {
        Long userId = Long.parseLong(principal.getName());

        UserSettingResponseDTO.UserSettingResponse response =
                userSettingService.getUserSettings(userId);

        return ApiResponse.onSuccess(response); //  "사용자 설정 조회 성공"
    }

    @Operation(
            summary = "유저 설정 수정",
            description = "현재 로그인한 사용자의 설정 정보를 수정합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "SETTING4003: 유효하지 않은 시간 형식"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패(토큰 누락/만료/위조)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "AUTH4006: 유저 없음 / SETTING4001: 설정 없음")
    })
    @PutMapping
    public ApiResponse<UserSettingResponseDTO.UserSettingResponse> updateUserSetting(
            Principal principal,
            @RequestBody @Valid UserSettingRequestDTO.UserSettingRequest request) {

        Long userId = Long.parseLong(principal.getName());
        UserSettingResponseDTO.UserSettingResponse response =
                userSettingService.updateUserSettings(userId, request);

        return ApiResponse.onSuccess(response);
    }

    @Operation(
            summary = "유저 설정 기본값 등록(멱등)",
            description = """
                현재 로그인한 사용자의 설정 정보를 기본값으로 등록합니다.
                - 이미 설정이 존재하면 아무 동작 없이 성공 응답을 반환합니다(멱등).
                """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패(토큰 누락/만료/위조)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "AUTH4006: 유저 없음")
    })
    @PostMapping("/register")
    public ApiResponse<Void> createDefaultUserSetting(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        userSettingService.createDefaultSetting(userId);

        return ApiResponse.onSuccess(null);
    }

    @Operation(
            summary = "신규 유저 여부 체크",
            description = "현재 로그인한 사용자가 설정값을 보유하고 있는지로 신규 유저 여부를 판단합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패(토큰 누락/만료/위조)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "AUTH4006: 유저 없음")
    })
    @GetMapping("/check-new")
    public ApiResponse<Boolean> checkNewUser(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        boolean isNewUser = userSettingService.isNewUser(userId);

        return ApiResponse.onSuccess(isNewUser);
    }

}
