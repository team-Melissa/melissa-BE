package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.service.payment.PaymentVerificationService;
import com.melissa.diary.web.dto.PaymentRequestDTO;
import com.melissa.diary.web.dto.PaymentResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@Tag(name = "PaymentAPI", description = "In-app purchase verification API")
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentVerificationService paymentVerificationService;

    @Operation(summary = "Verify Google Play remove_ads purchase")
    @PostMapping("/google/verify")
    public ApiResponse<PaymentResponseDTO.VerifyResponse> verifyGoogle(
            Principal principal,
            @Valid @RequestBody PaymentRequestDTO.GoogleVerifyRequest request
    ) {
        Long userId = Long.parseLong(principal.getName());
        return ApiResponse.onSuccess(paymentVerificationService.verifyGoogle(userId, request));
    }

    @Operation(summary = "Restore Google Play remove_ads purchases")
    @PostMapping("/google/restore")
    public ApiResponse<PaymentResponseDTO.RestoreResponse> restoreGoogle(
            Principal principal,
            @Valid @RequestBody PaymentRequestDTO.GoogleRestoreRequest request
    ) {
        Long userId = Long.parseLong(principal.getName());
        return ApiResponse.onSuccess(paymentVerificationService.restoreGoogle(userId, request));
    }

    @Operation(summary = "Verify Apple App Store remove_ads transaction")
    @PostMapping("/apple/verify")
    public ApiResponse<PaymentResponseDTO.VerifyResponse> verifyApple(
            Principal principal,
            @Valid @RequestBody PaymentRequestDTO.AppleVerifyRequest request
    ) {
        Long userId = Long.parseLong(principal.getName());
        return ApiResponse.onSuccess(paymentVerificationService.verifyApple(userId, request));
    }

    @Operation(summary = "Restore Apple App Store remove_ads transactions")
    @PostMapping("/apple/restore")
    public ApiResponse<PaymentResponseDTO.RestoreResponse> restoreApple(
            Principal principal,
            @Valid @RequestBody PaymentRequestDTO.AppleRestoreRequest request
    ) {
        Long userId = Long.parseLong(principal.getName());
        return ApiResponse.onSuccess(paymentVerificationService.restoreApple(userId, request));
    }
}
