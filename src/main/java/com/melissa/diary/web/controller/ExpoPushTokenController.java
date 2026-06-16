package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.service.ExpoPushTokenService;
import com.melissa.diary.service.IdempotencyService;
import com.melissa.diary.web.dto.ExpoPushTokenRequestDTO;
import com.melissa.diary.web.dto.ExpoPushTokenResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@Tag(name = "ExpoPushTokenAPI", description = "Expo Push Token 관리 API")
@RequestMapping("/api/v1/expo-push-tokens")
@RequiredArgsConstructor
@Slf4j
public class ExpoPushTokenController {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final String REGISTER_TOKEN_ENDPOINT = "POST:/api/v1/expo-push-tokens";
    private static final String DELETE_TOKEN_ENDPOINT = "DELETE:/api/v1/expo-push-tokens";

    private final ExpoPushTokenService expoPushTokenService;
    private final IdempotencyService idempotencyService;

    @Operation(
            summary = "Expo Push Token 등록/갱신",
            description = """
                    현재 로그인한 사용자의 Expo Push Token을 등록하거나 기존 토큰 정보를 갱신합니다.
                    - 인증 필요 (Bearer JWT)
                    - userId는 토큰(subject) 기반으로 서버에서 결정
                    - `platform` 후보: ANDROID, IOS
                    - `expoPushToken` 형식: ExponentPushToken[...]
                    - `Idempotency-Key` 헤더를 전달하면 같은 요청의 중복 처리를 방지합니다.
                    """
    )
    @PostMapping
    public ApiResponse<ExpoPushTokenResponseDTO.TokenResponse> registerToken(
            Principal principal,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
            @Valid @RequestBody ExpoPushTokenRequestDTO.RegisterTokenRequest request) {

        Long userId = Long.parseLong(principal.getName());
        log.info("[ExpoPushTokenController] register token request. userId={}, platform={}",
                userId, request.getPlatform());

        if (isBlank(idempotencyKey)) {
            ExpoPushTokenResponseDTO.TokenResponse response = expoPushTokenService.registerToken(userId, request);
            return ApiResponse.onSuccess(response);
        }

        var claim = idempotencyService.claim(userId, REGISTER_TOKEN_ENDPOINT, idempotencyKey, request);
        if (claim.getAction() == IdempotencyService.ClaimAction.RETURN_CACHED) {
            ExpoPushTokenResponseDTO.TokenResponse cached = idempotencyService.deserialize(
                    claim.getRecord().getResponseBody(),
                    ExpoPushTokenResponseDTO.TokenResponse.class
            );
            return ApiResponse.onSuccess(cached);
        }

        try {
            ExpoPushTokenResponseDTO.TokenResponse response = expoPushTokenService.registerToken(userId, request);
            idempotencyService.markSucceeded(claim.getRecord().getId(), idempotencyService.serialize(response));
            return ApiResponse.onSuccess(response);
        } catch (RuntimeException e) {
            idempotencyService.markFailedRetryable(claim.getRecord().getId(), e);
            throw e;
        }
    }

    @Operation(
            summary = "내 Expo Push Token 목록 조회",
            description = """
                    현재 로그인한 사용자에게 연결된 Expo Push Token 목록을 반환합니다.
                    - 인증 필요 (Bearer JWT)
                    - `platform` 후보: ANDROID, IOS
                    - `invalid=true`이면 발송 실패 등으로 무효 처리된 토큰입니다.
                    """
    )
    @GetMapping
    public ApiResponse<List<ExpoPushTokenResponseDTO.TokenResponse>> getUserTokens(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        log.info("[ExpoPushTokenController] get tokens request. userId={}", userId);

        List<ExpoPushTokenResponseDTO.TokenResponse> tokens = expoPushTokenService.getUserTokens(userId);
        return ApiResponse.onSuccess(tokens);
    }

    @Operation(
            summary = "Expo Push Token 삭제",
            description = """
                    특정 Expo Push Token을 삭제합니다.
                    - 인증 필요 (Bearer JWT)
                    - 삭제 대상 토큰은 path variable로 전달합니다.
                    - `Idempotency-Key` 헤더를 전달하면 같은 삭제 요청의 중복 처리를 방지합니다.
                    """
    )
    @DeleteMapping("/{expoPushToken}")
    public ApiResponse<ExpoPushTokenResponseDTO.DeleteResponse> deleteToken(
            @Parameter(description = "삭제할 Expo Push Token. 형식: ExponentPushToken[...]", required = true,
                    example = "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]")
            @PathVariable String expoPushToken,
            @RequestHeader(value = IDEMPOTENCY_HEADER, required = false) String idempotencyKey,
            Principal principal) {

        Long userId = Long.parseLong(principal.getName());
        log.info("[ExpoPushTokenController] delete token request. userId={}, token={}", userId, expoPushToken);

        if (isBlank(idempotencyKey)) {
            ExpoPushTokenResponseDTO.DeleteResponse response = expoPushTokenService.deleteToken(expoPushToken);
            return ApiResponse.onSuccess(response);
        }

        var claim = idempotencyService.claim(userId, DELETE_TOKEN_ENDPOINT, idempotencyKey, expoPushToken);
        if (claim.getAction() == IdempotencyService.ClaimAction.RETURN_CACHED) {
            ExpoPushTokenResponseDTO.DeleteResponse cached = idempotencyService.deserialize(
                    claim.getRecord().getResponseBody(),
                    ExpoPushTokenResponseDTO.DeleteResponse.class
            );
            return ApiResponse.onSuccess(cached);
        }

        try {
            ExpoPushTokenResponseDTO.DeleteResponse response = expoPushTokenService.deleteToken(expoPushToken);
            idempotencyService.markSucceeded(claim.getRecord().getId(), idempotencyService.serialize(response));
            return ApiResponse.onSuccess(response);
        } catch (RuntimeException e) {
            idempotencyService.markFailedRetryable(claim.getRecord().getId(), e);
            throw e;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
