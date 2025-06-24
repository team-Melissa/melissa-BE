package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.service.AiProfileServiceV2;
import com.melissa.diary.web.dto.AiProfileRequestDTO;
import com.melissa.diary.web.dto.AiProfileResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@Tag(name = "AiProfileAPI-V2", description = "AI 프로필 생성 – 텍스트 즉시 · 이미지 비동기")
@RequestMapping("/api/v2/ai-profiles")
@RequiredArgsConstructor
public class AiProfileV2Controller {

    private final @Qualifier("aiProfileV2") AiProfileServiceV2 profileService;

    @Operation(description = "V2: LLM 프로필 텍스트만 즉시 반환, 이미지 비동기")
    @PostMapping
    public ApiResponse<AiProfileResponseDTO.AiProfileResponse> createProfileV2(
            Principal principal,
            @RequestBody AiProfileRequestDTO.AiProfileCreateRequest request) {

        Long userId = Long.parseLong(principal.getName());
        var dto = profileService.createAiProfile(userId, request);  // imageUrl == null
        return ApiResponse.onSuccess(dto);
    }

}
