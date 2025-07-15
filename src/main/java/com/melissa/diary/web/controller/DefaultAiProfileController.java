package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.service.DefaultAiProfileService;
import com.melissa.diary.web.dto.AiProfileResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@Tag(name = "DefaultAiProfileAPI", description = "기본 제공 AI 프로필 관련 API")
@RequestMapping("/api/v1/default-ai-profiles")
@RequiredArgsConstructor
public class DefaultAiProfileController {

    private final DefaultAiProfileService defaultAiProfileService;

    @Operation(description = "기본 제공 AI 프로필 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<AiProfileResponseDTO.AiProfileResponse>> getDefaultProfileList(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        defaultAiProfileService.syncUserDefaultProfileMappings(userId);
        List<AiProfileResponseDTO.AiProfileResponse> list = defaultAiProfileService.getDefaultProfileList();
        return ApiResponse.onSuccess(list);
    }

    @Operation(description = "기본 제공 AI 프로필 단건을 조회합니다.")
    @GetMapping("/{defaultProfileId}")
    public ApiResponse<AiProfileResponseDTO.AiProfileResponse> getDefaultProfile(Principal principal, @PathVariable Long defaultProfileId) {
        Long userId = Long.parseLong(principal.getName());
        defaultAiProfileService.syncUserDefaultProfileMappings(userId);
        AiProfileResponseDTO.AiProfileResponse response = defaultAiProfileService.getDefaultProfileOrThrow(defaultProfileId);
        return ApiResponse.onSuccess(response);
    }

    @Operation(description = "사용자별 기본 제공 AI 프로필을 비활성화(삭제)합니다.")
    @DeleteMapping("/{defaultProfileId}")
    public ApiResponse<Void> deleteUserDefaultProfile(Principal principal, @PathVariable Long defaultProfileId) {
        Long userId = Long.parseLong(principal.getName());
        defaultAiProfileService.deleteUserDefaultProfileOrThrow(userId, defaultProfileId);
        return ApiResponse.onSuccess(null);
    }
} 