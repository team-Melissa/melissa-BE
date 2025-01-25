package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.service.AiProfileService;
import com.melissa.diary.web.dto.AiProfileRequestDTO;
import com.melissa.diary.web.dto.AiProfileResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

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
}
