package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.service.AiProfileService;
import com.melissa.diary.web.dto.AiProfileResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@Tag(name = "AiProfileAPI", description = "AI 프로필 관련 API (전역 공유 리소스)")
@RequestMapping("/api")
@RequiredArgsConstructor
public class AiProfileController {

    private final AiProfileService aiProfileService;

    @Operation(summary = "AI 프로필 상세 조회",
               description = "특정 AI 프로필을 상세 조회합니다. (ID 1~5)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "AUTH4006: 사용자를 찾을 수 없음 / PROFILE4002: AI 프로필을 찾을 수 없음")
    })
    @GetMapping("/v1/ai-profiles/{aiProfileId}")
    public ApiResponse<AiProfileResponseDTO.AiProfileResponse> getAiProfile(
            @Parameter(description = "AI 프로필 ID", required = true, example = "1")
            @PathVariable(name = "aiProfileId") Long aiProfileId, 
            Principal principal) {

        Long userId = Long.parseLong(principal.getName());
        AiProfileResponseDTO.AiProfileResponse response = aiProfileService.getAiProfile(userId, aiProfileId);
        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "최근 사용 AI 프로필 조회",
               description = "최근 사용한 AI 프로필 ID를 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "PROFILE4002: AI 프로필을 찾을 수 없음")
    })
    @GetMapping("/v1/ai-profiles/recent")
    public ApiResponse<AiProfileResponseDTO.AiProfileResponse> getAiProfileIdRecent(
            Principal principal) {

        Long userId = Long.parseLong(principal.getName());
        AiProfileResponseDTO.AiProfileResponse response = aiProfileService.getRecentAiProfileId(userId);

        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "AI 프로필 생성 질문 조회",
               description = "특정 AI 프로필을 만들 당시의 질문을 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "AUTH4006: 사용자를 찾을 수 없음 / PROFILE4002: AI 프로필을 찾을 수 없음")
    })
    @GetMapping("/v1/ai-profiles/{aiProfileId}/question")
    public ApiResponse<AiProfileResponseDTO.AiProfileQuestionResponse> getAiQuestionProfile(
            @Parameter(description = "AI 프로필 ID", required = true, example = "1")
            @PathVariable(name = "aiProfileId") Long aiProfileId, 
            Principal principal) {

        Long userId = Long.parseLong(principal.getName());
        AiProfileResponseDTO.AiProfileQuestionResponse response= aiProfileService.getAiProfileQuestion(userId, aiProfileId);
        return ApiResponse.onSuccess(response);
    }

    @Operation(summary = "AI 프로필 목록 조회",
               description = "전체 AI 프로필 목록을 조회합니다. (ID 1~5 고정)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "AUTH4006: 사용자를 찾을 수 없음")
    })
    @GetMapping("/v1/ai-profiles")
    public ApiResponse<List<AiProfileResponseDTO.AiProfileResponse>> getAiProfileList(
            Principal principal) {

        Long userId = Long.parseLong(principal.getName());
        List<AiProfileResponseDTO.AiProfileResponse> list = aiProfileService.getAiProfileList(userId);
        return ApiResponse.onSuccess(list);
    }
}
