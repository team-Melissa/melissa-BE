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
@Tag(name = "ExpoPushTokenAPI", description = "Expo Push token management API")
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
            summary = "Register Expo Push Token",
            description = "Registers or updates the Expo Push Token for the authenticated user."
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
            summary = "Get Expo Push Tokens",
            description = "Returns all Expo Push Tokens registered for the authenticated user."
    )
    @GetMapping
    public ApiResponse<List<ExpoPushTokenResponseDTO.TokenResponse>> getUserTokens(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        log.info("[ExpoPushTokenController] get tokens request. userId={}", userId);

        List<ExpoPushTokenResponseDTO.TokenResponse> tokens = expoPushTokenService.getUserTokens(userId);
        return ApiResponse.onSuccess(tokens);
    }

    @Operation(
            summary = "Delete Expo Push Token",
            description = "Deletes a specific Expo Push Token."
    )
    @DeleteMapping("/{expoPushToken}")
    public ApiResponse<ExpoPushTokenResponseDTO.DeleteResponse> deleteToken(
            @Parameter(description = "Expo Push Token to delete", required = true,
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
