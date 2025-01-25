package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.domain.UserSetting;
import com.melissa.diary.service.UserSettingService;
import com.melissa.diary.web.dto.UserSettingRequestDTO;
import com.melissa.diary.web.dto.UserSettingResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(description = "현재 로그인한 유저의 설정 정보를 조회합니다.")
    @GetMapping
    public ApiResponse<UserSettingResponseDTO.UserSettingResponse> getUserSetting(Principal principal) {
        Long userId = Long.parseLong(principal.getName());

        UserSettingResponseDTO.UserSettingResponse response =
                userSettingService.getUserSettings(userId);

        return ApiResponse.onSuccess(response); //  "사용자 설정 조회 성공"
    }

    @Operation(description = "유저의 설정정보를 수정합니다.")
    @PutMapping
    public ApiResponse<UserSettingResponseDTO.UserSettingResponse> updateUserSetting(
            Principal principal,
            @RequestBody @Valid UserSettingRequestDTO.UserSettingRequest request) {

        Long userId = Long.parseLong(principal.getName());
        UserSettingResponseDTO.UserSettingResponse response =
                userSettingService.updateUserSettings(userId, request);

        return ApiResponse.onSuccess(response);
    }

    @Operation(description = "유저의 설정정보를 기본값으로 등록합니다.")
    @PostMapping("/register")
    public ApiResponse<Void> createDefaultUserSetting(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        userSettingService.createDefaultSetting(userId);

        return ApiResponse.onSuccess(null);
    }

    @Operation(description = "신규유저인지 체크합니다.")
    @GetMapping("/check-new")
    public ApiResponse<Boolean> checkNewUser(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        boolean isNewUser = userSettingService.isNewUser(userId);

        return ApiResponse.onSuccess(isNewUser);
    }

}
