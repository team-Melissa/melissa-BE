package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.service.AiProfileService;
import com.melissa.diary.web.dto.AiProfileRequestDTO;
import com.melissa.diary.web.dto.AiProfileResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@Tag(name = "AiProfileAPI", description = "AI 프로필 관련 API")
@RequestMapping("/api/v1/ai-profiles")
@RequiredArgsConstructor
public class AiProfileController {

    private final AiProfileService aiProfileService;

    @Operation(description = "6가지 질문을 기반으로 LLM을 호출하여 AI 프로필을 생성합니다.")
    @PostMapping
    public ApiResponse<AiProfileResponseDTO.AiProfileResponse> createAiProfile(
            Principal principal,
            @RequestBody AiProfileRequestDTO.AiProfileCreateRequest request) {

        Long userId = Long.parseLong(principal.getName());
        AiProfileResponseDTO.AiProfileResponse response = aiProfileService.createAiProfile(userId,request);

        return ApiResponse.onSuccess(response);
    }

    @Operation(description = "특정 AI 프로필을 상세 조회합니다.")
    @GetMapping("/{aiProfileId}")
    public ApiResponse<AiProfileResponseDTO.AiProfileResponse> getAiProfile(

            @PathVariable(name = "aiProfileId") Long aiProfileId, Principal principal) {

        Long userId = Long.parseLong(principal.getName());
        AiProfileResponseDTO.AiProfileResponse response =
                aiProfileService.getAiProfile(userId, aiProfileId);
        return ApiResponse.onSuccess(response);
    }

    @Operation(description = "특정 AI 프로필을 만들 당시의 질문을 조회합니다.")
    @GetMapping("/{aiProfileId}/question")
    public ApiResponse<AiProfileResponseDTO.AiProfileQuestionResponse> getAiQuestionProfile(

            @PathVariable(name = "aiProfileId") Long aiProfileId, Principal principal) {

        Long userId = Long.parseLong(principal.getName());
        AiProfileResponseDTO.AiProfileQuestionResponse response=
                aiProfileService.getAiProfileQuestion(userId, aiProfileId);
        return ApiResponse.onSuccess(response);
    }

    @Operation(description = "AI 프로필을 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<AiProfileResponseDTO.AiProfileResponse>> getAiProfileList(Principal principal) {

        Long userId = Long.parseLong(principal.getName());
        List<AiProfileResponseDTO.AiProfileResponse> list = aiProfileService.getAiProfileList(userId);
        return ApiResponse.onSuccess(list);
    }
    @Operation(description = "해당하는 AI 프로필을 삭제합니다.")
    @DeleteMapping("/{aiProfileId}")
    public ApiResponse<Void> deleteAiProfile(
            Principal principal,
            @PathVariable(name = "aiProfileId") Long aiProfileId) {

        Long userId = Long.parseLong(principal.getName());
        aiProfileService.deleteAiProfile(userId, aiProfileId);
        return ApiResponse.onSuccess(null);
    }

}
