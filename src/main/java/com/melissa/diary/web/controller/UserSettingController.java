package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.domain.UserSetting;
import com.melissa.diary.service.UserSettingService;
import com.melissa.diary.web.dto.UserSettingResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
}
