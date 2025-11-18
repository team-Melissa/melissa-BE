package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.service.ExpoPushTokenService;
import com.melissa.diary.web.dto.ExpoPushTokenRequestDTO;
import com.melissa.diary.web.dto.ExpoPushTokenResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@Tag(name = "ExpoPushTokenAPI", description = "Expo Push 알림 토큰 관리 API")
@RequestMapping("/api/v1/expo-push-tokens")
@RequiredArgsConstructor
@Slf4j
public class ExpoPushTokenController {
    
    private final ExpoPushTokenService expoPushTokenService;
    
    /**
     * Expo Push Token 등록 (로그인 사용자)
     */
    @Operation(
            summary = "Expo Push Token 등록",
            description = "로그인한 사용자의 Expo Push Token을 등록합니다. 이미 존재하는 토큰이면 사용자 정보를 업데이트합니다."
    )
    @PostMapping
    public ApiResponse<ExpoPushTokenResponseDTO.TokenResponse> registerToken(
            Principal principal,
            @Valid @RequestBody ExpoPushTokenRequestDTO.RegisterTokenRequest request) {
        
        Long userId = Long.parseLong(principal.getName());
        log.info("[ExpoPushTokenController] 토큰 등록 요청. userId={}, platform={}", 
                userId, request.getPlatform());
        
        ExpoPushTokenResponseDTO.TokenResponse response = 
                expoPushTokenService.registerToken(userId, request);
        
        return ApiResponse.onSuccess(response);
    }
    
    /**
     * 사용자별 토큰 목록 조회
     */
    @Operation(
            summary = "내 Expo Push Token 목록 조회",
            description = "현재 로그인한 사용자의 등록된 모든 Expo Push Token을 조회합니다."
    )
    @GetMapping
    public ApiResponse<List<ExpoPushTokenResponseDTO.TokenResponse>> getUserTokens(
            Principal principal) {
        
        Long userId = Long.parseLong(principal.getName());
        log.info("[ExpoPushTokenController] 토큰 목록 조회 요청. userId={}", userId);
        
        List<ExpoPushTokenResponseDTO.TokenResponse> tokens = 
                expoPushTokenService.getUserTokens(userId);
        
        return ApiResponse.onSuccess(tokens);
    }
    
    /**
     * 특정 토큰 삭제
     */
    @Operation(
            summary = "Expo Push Token 삭제",
            description = "특정 Expo Push Token을 삭제합니다. 더 이상 해당 기기로 알림이 발송되지 않습니다."
    )
    @DeleteMapping("/{expoPushToken}")
    public ApiResponse<ExpoPushTokenResponseDTO.DeleteResponse> deleteToken(
            @Parameter(description = "삭제할 Expo Push Token", required = true)
            @PathVariable String expoPushToken) {
        
        log.info("[ExpoPushTokenController] 토큰 삭제 요청. token={}", expoPushToken);
        
        ExpoPushTokenResponseDTO.DeleteResponse response = 
                expoPushTokenService.deleteToken(expoPushToken);
        
        return ApiResponse.onSuccess(response);
    }
}

