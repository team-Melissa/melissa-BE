package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.service.AiProfileService;
import com.melissa.diary.service.AiProfileServiceV2;
import com.melissa.diary.web.dto.AiProfileRequestDTO;
import com.melissa.diary.web.dto.AiProfileResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * AI 프로필 관리 API
 * v1: 이미지 동기 생성 (deprecated)
 * v2: 이미지 비동기 생성 (권장)
 */
@RestController
@Tag(name = "AI Profile API", description = "AI 프로필 관리 API (v1, v2)")
@RequiredArgsConstructor
public class AiProfileController {

    private final AiProfileService aiProfileService;
    private final @Qualifier("aiProfileV2") AiProfileServiceV2 aiProfileServiceV2;

    // ============================================
    // v1 API (Deprecated - 동기 처리)
    // ============================================

    @Operation(summary = "[v1 - Deprecated] AI 프로필 생성", 
               description = "⚠️ 이미지를 동기로 생성하여 응답이 느립니다. v2 API 사용을 권장합니다.")
    @PostMapping("/api/v1/ai-profiles")
    @Deprecated(since = "1.2.0", forRemoval = true)
    public ApiResponse<AiProfileResponseDTO.AiProfileResponse> createAiProfile(
            Principal principal,
            @RequestBody AiProfileRequestDTO.AiProfileCreateRequest request) {

        Long userId = Long.parseLong(principal.getName());
        AiProfileResponseDTO.AiProfileResponse response = aiProfileService.createAiProfile(userId, request);

        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "[v1] AI 프로필 상세 조회", 
               description = "특정 AI 프로필을 상세 조회합니다.")
    @GetMapping("/api/v1/ai-profiles/{aiProfileId}")
    public ApiResponse<AiProfileResponseDTO.AiProfileResponse> getAiProfile(
            @PathVariable(name = "aiProfileId") Long aiProfileId, 
            Principal principal) {

        Long userId = Long.parseLong(principal.getName());
        AiProfileResponseDTO.AiProfileResponse response = aiProfileService.getAiProfile(userId, aiProfileId);
        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "[v1] 최근 사용 AI 프로필 조회", 
               description = "최근 사용한 AI 프로필 ID를 조회합니다.")
    @GetMapping("/api/v1/ai-profiles/recent")
    public ApiResponse<AiProfileResponseDTO.AiProfileResponse> getAiProfileIdRecent(
            Principal principal) {

        Long userId = Long.parseLong(principal.getName());
        AiProfileResponseDTO.AiProfileResponse response = aiProfileService.getRecentAiProfileId(userId);

        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "[v1] AI 프로필 생성 질문 조회", 
               description = "특정 AI 프로필을 만들 당시의 질문을 조회합니다.")
    @GetMapping("/api/v1/ai-profiles/{aiProfileId}/question")
    public ApiResponse<AiProfileResponseDTO.AiProfileQuestionResponse> getAiQuestionProfile(
            @PathVariable(name = "aiProfileId") Long aiProfileId, 
            Principal principal) {

        Long userId = Long.parseLong(principal.getName());
        AiProfileResponseDTO.AiProfileQuestionResponse response = aiProfileService.getAiProfileQuestion(userId, aiProfileId);
        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "[v1] AI 프로필 목록 조회", 
               description = "사용자의 AI 프로필 목록을 조회합니다.")
    @GetMapping("/api/v1/ai-profiles")
    public ApiResponse<List<AiProfileResponseDTO.AiProfileResponse>> getAiProfileList(
            Principal principal) {

        Long userId = Long.parseLong(principal.getName());
        // 기본 제공 프로필 동기화
        aiProfileService.syncUserDefaultProfiles(userId);
        List<AiProfileResponseDTO.AiProfileResponse> list = aiProfileService.getAiProfileList(userId);
        return ApiResponse.onSuccess(list);
    }

    @Operation(summary = "[v1] AI 프로필 삭제", 
               description = "해당하는 AI 프로필을 삭제합니다.")
    @DeleteMapping("/api/v1/ai-profiles/{aiProfileId}")
    public ApiResponse<Void> deleteAiProfile(
            Principal principal,
            @PathVariable(name = "aiProfileId") Long aiProfileId) {

        Long userId = Long.parseLong(principal.getName());
        aiProfileService.deleteAiProfile(userId, aiProfileId);
        return ApiResponse.onSuccess(null);
    }

    @Operation(summary = "[v1] 기본 프로필 복원", 
               description = "사용자의 기본 제공 AI 프로필을 모두 복원합니다.")
    @PatchMapping("/api/v1/ai-profiles/restore")
    public ApiResponse<Void> restoreDefaults(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        aiProfileService.restoreUserDefaultProfiles(userId);
        return ApiResponse.onSuccess(null);
    }

    // ============================================
    // v2 API (Current - 비동기 처리, 권장)
    // ============================================

    @Operation(summary = "[v2 - 권장] AI 프로필 생성", 
               description = "✨ 이미지를 비동기로 생성하여 빠른 응답을 제공합니다. 텍스트 정보만 즉시 반환되며, 이미지는 생성 완료 후 조회 가능합니다.")
    @PostMapping("/api/v2/ai-profiles")
    public ApiResponse<AiProfileResponseDTO.AiProfileResponse> createAiProfileV2(
            Principal principal,
            @RequestBody AiProfileRequestDTO.AiProfileCreateRequest request) {

        Long userId = Long.parseLong(principal.getName());
        AiProfileResponseDTO.AiProfileResponse response = aiProfileServiceV2.createAiProfile(userId, request);

        return ApiResponse.onSuccess(response);
    }

}
