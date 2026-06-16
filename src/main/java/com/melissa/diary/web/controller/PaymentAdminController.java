package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.service.payment.PaymentAdminRefundService;
import com.melissa.diary.web.dto.PaymentRequestDTO;
import com.melissa.diary.web.dto.PaymentResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@Tag(name = "PaymentAdminAPI", description = "결제 관리자 fallback API")
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
public class PaymentAdminController {

    private final PaymentAdminRefundService paymentAdminRefundService;

    @Operation(
            summary = "관리자 수동 환불 반영",
            description = """
                스토어 콘솔에서 이미 환불 처리된 결제를 백엔드 DB에 수동 반영하고 광고 제거 권한을 회수합니다.
                - 관리자 인증 필요 (Bearer JWT, ADMIN 계정)
                - Google/Apple 스토어에 환불을 요청하는 API가 아니라 내부 fallback 처리 API
                - paymentId 기준으로 결제 상태를 `REFUNDED`로 변경
                - 해당 결제가 부여한 `REMOVE_ADS` 권한을 비활성화
                - 이미 환불 처리된 결제는 `alreadyProcessed=true`로 멱등 응답
                """
    )
    @PostMapping("/{paymentId}/refund")
    public ApiResponse<PaymentResponseDTO.AdminManualRefundResponse> refundManually(
            Principal principal,
            @PathVariable Long paymentId,
            @Valid @RequestBody PaymentRequestDTO.AdminManualRefundRequest request
    ) {
        Long adminUserId = Long.parseLong(principal.getName());
        return ApiResponse.onSuccess(paymentAdminRefundService.refundManually(adminUserId, paymentId, request));
    }
}
