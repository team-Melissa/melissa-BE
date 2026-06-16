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
@Tag(name = "PaymentAPI", description = "인앱결제 검증 및 구매 복원 API")
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentVerificationService paymentVerificationService;

    @Operation(
            summary = "Google Play 광고 제거 구매 검증",
            description = """
                Google Play Billing에서 받은 비소모성 상품 구매 정보를 서버에서 검증하고 광고 제거 권한을 부여합니다.
                - 인증 필요 (Bearer JWT)
                - userId는 토큰(subject) 기반으로 서버에서 결정
                - 요청 productId는 Google Play 상품 ID인 `premium`
                - 검증 성공 시 응답의 `entitlement.active=true`로 광고 제거 적용 가능
                - 같은 구매를 같은 사용자가 다시 검증하면 `alreadyProcessed=true`로 응답
                """
    )
    @PostMapping("/google/verify")
    public ApiResponse<PaymentResponseDTO.VerifyResponse> verifyGoogle(
            Principal principal,
            @Valid @RequestBody PaymentRequestDTO.GoogleVerifyRequest request
    ) {
        Long userId = Long.parseLong(principal.getName());
        return ApiResponse.onSuccess(paymentVerificationService.verifyGoogle(userId, request));
    }

    @Operation(
            summary = "Google Play 광고 제거 구매 복원",
            description = """
                Google Play Billing에서 조회한 보유 구매 목록을 서버에 다시 검증하여 광고 제거 권한을 복원합니다.
                - 인증 필요 (Bearer JWT)
                - userId는 토큰(subject) 기반으로 서버에서 결정
                - 일부 구매 검증 실패가 있어도 API 자체는 성공할 수 있으며 실패 항목은 `failed`에 포함
                - 최종 광고 제거 여부는 `features.adRemoved` 값으로 판단
                """
    )
    @PostMapping("/google/restore")
    public ApiResponse<PaymentResponseDTO.RestoreResponse> restoreGoogle(
            Principal principal,
            @Valid @RequestBody PaymentRequestDTO.GoogleRestoreRequest request
    ) {
        Long userId = Long.parseLong(principal.getName());
        return ApiResponse.onSuccess(paymentVerificationService.restoreGoogle(userId, request));
    }

    @Operation(
            summary = "Apple App Store 광고 제거 구매 검증",
            description = """
                App Store에서 받은 비소모성 상품 transactionId를 서버에서 검증하고 광고 제거 권한을 부여합니다.
                - 인증 필요 (Bearer JWT)
                - userId는 토큰(subject) 기반으로 서버에서 결정
                - 요청 productId는 Apple 상품 ID인 `com.melissa.melissaFE.premium`
                - environment는 테스트 결제 `SANDBOX`, 상용 결제 `PRODUCTION`
                - 검증 성공 시 응답의 `entitlement.active=true`로 광고 제거 적용 가능
                """
    )
    @PostMapping("/apple/verify")
    public ApiResponse<PaymentResponseDTO.VerifyResponse> verifyApple(
            Principal principal,
            @Valid @RequestBody PaymentRequestDTO.AppleVerifyRequest request
    ) {
        Long userId = Long.parseLong(principal.getName());
        return ApiResponse.onSuccess(paymentVerificationService.verifyApple(userId, request));
    }

    @Operation(
            summary = "Apple App Store 광고 제거 구매 복원",
            description = """
                App Store에서 조회한 보유 거래 목록을 서버에 다시 검증하여 광고 제거 권한을 복원합니다.
                - 인증 필요 (Bearer JWT)
                - userId는 토큰(subject) 기반으로 서버에서 결정
                - 일부 거래 검증 실패가 있어도 API 자체는 성공할 수 있으며 실패 항목은 `failed`에 포함
                - 최종 광고 제거 여부는 `features.adRemoved` 값으로 판단
                """
    )
    @PostMapping("/apple/restore")
    public ApiResponse<PaymentResponseDTO.RestoreResponse> restoreApple(
            Principal principal,
            @Valid @RequestBody PaymentRequestDTO.AppleRestoreRequest request
    ) {
        Long userId = Long.parseLong(principal.getName());
        return ApiResponse.onSuccess(paymentVerificationService.restoreApple(userId, request));
    }
}
