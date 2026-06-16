package com.melissa.diary.web.controller;

import com.melissa.diary.apiPayload.ApiResponse;
import com.melissa.diary.service.TermAgreementService;
import com.melissa.diary.web.dto.TermRequestDTO;
import com.melissa.diary.web.dto.TermResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@Tag(name = "TermAPI", description = "약관 동의 API")
@RequestMapping("/api/v1/terms")
@RequiredArgsConstructor
public class TermController {

    private final TermAgreementService termAgreementService;

    @Operation(
            summary = "내 약관 동의 상태 조회",
            description = """
                    현재 로그인한 사용자의 약관 동의 상태와 서비스 진입 차단 여부를 반환합니다.
                    - 인증 필요 (Bearer JWT)
                    - 약관 본문은 포함하지 않습니다.
                    - 프론트는 `agreementRequired`와 각 약관의 `blocking`, `action`을 기준으로 분기합니다.
                    - `reason` 후보: NONE, INITIAL_REQUIRED_TERMS, UPDATED_REQUIRED_TERMS, OPTIONAL_TERMS_AVAILABLE
                    - `submitContext` 후보: NONE, SIGNUP, RECONSENT, OPTIONAL_UPDATE
                    - `terms[].termCode` 초기 후보: SERVICE_TERMS, PRIVACY_POLICY, MARKETING
                    - `terms[].action` 후보: NONE, INITIAL_AGREEMENT_REQUIRED, RECONSENT_REQUIRED, OPTIONAL_CONSENT_AVAILABLE, OPTIONAL_RECONSENT_AVAILABLE
                    """
    )
    @GetMapping("/agreement-status")
    public ApiResponse<TermResponseDTO.AgreementStatusResponse> getAgreementStatus(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        return ApiResponse.onSuccess(termAgreementService.getAgreementStatus(userId));
    }

    @Operation(
            summary = "약관 동의 화면 조회",
            description = """
                    현재 로그인한 사용자의 약관 동의 화면 구성 데이터를 반환합니다.
                    - 인증 필요 (Bearer JWT)
                    - 약관 본문을 포함합니다.
                    - 프론트 약관 동의 화면 렌더링용 API입니다.
                    - 응답 구조는 agreement-status와 같고 `terms[].content`가 포함됩니다.
                    - `reason` 후보: NONE, INITIAL_REQUIRED_TERMS, UPDATED_REQUIRED_TERMS, OPTIONAL_TERMS_AVAILABLE
                    - `submitContext` 후보: NONE, SIGNUP, RECONSENT, OPTIONAL_UPDATE
                    - `terms[].termCode` 초기 후보: SERVICE_TERMS, PRIVACY_POLICY, MARKETING
                    - `terms[].action` 후보: NONE, INITIAL_AGREEMENT_REQUIRED, RECONSENT_REQUIRED, OPTIONAL_CONSENT_AVAILABLE, OPTIONAL_RECONSENT_AVAILABLE
                    """
    )
    @GetMapping("/agreement-screen")
    public ApiResponse<TermResponseDTO.AgreementStatusResponse> getAgreementScreen(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        return ApiResponse.onSuccess(termAgreementService.getAgreementScreen(userId));
    }

    @Operation(
            summary = "약관 버전 본문 조회",
            description = """
                    특정 약관 버전의 상세 본문을 반환합니다.
                    - 인증 필요 (Bearer JWT)
                    - 발행되지 않았거나 아직 효력이 발생하지 않은 버전은 조회할 수 없습니다.
                    - `termCode` 초기 후보: SERVICE_TERMS, PRIVACY_POLICY, MARKETING
                    - `contentFormat` 후보: TEXT, HTML, MARKDOWN
                    """
    )
    @GetMapping("/versions/{termVersionId}")
    public ApiResponse<TermResponseDTO.TermVersionDetailResponse> getTermVersion(
            @PathVariable Long termVersionId
    ) {
        return ApiResponse.onSuccess(termAgreementService.getTermVersion(termVersionId));
    }

    @Operation(
            summary = "약관 동의 제출",
            description = """
                    현재 로그인한 사용자의 약관 동의/거절 결정을 저장합니다.
                    - 인증 필요 (Bearer JWT)
                    - 필수 약관은 반드시 true로 제출해야 합니다.
                    - 선택 약관은 false 제출이 가능하며 이력으로 저장됩니다.
                    - `context` 후보: SIGNUP, RECONSENT, OPTIONAL_UPDATE, WITHDRAWAL
                    - `termVersionId`는 agreement-status 또는 agreement-screen의 `terms[].currentTermVersionId` 값을 사용합니다.
                    - 필수 약관 누락 또는 필수 약관 `agreed=false` 제출 시 400으로 응답합니다.
                    """
    )
    @PostMapping("/agreements")
    public ApiResponse<TermResponseDTO.SubmitAgreementsResponse> submitAgreements(
            Principal principal,
            @Valid @RequestBody TermRequestDTO.SubmitAgreementsRequest request
    ) {
        Long userId = Long.parseLong(principal.getName());
        return ApiResponse.onSuccess(termAgreementService.submitAgreements(userId, request));
    }
}
