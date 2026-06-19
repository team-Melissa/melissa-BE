package com.melissa.diary.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class TermResponseDTO {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "약관 동의 상태 응답")
    public static class AgreementStatusResponse {

        @Schema(description = "서비스 진입 전 약관 동의 화면이 필요한지 여부. true면 서비스 진입 전 약관 동의 화면으로 이동", example = "true")
        private Boolean agreementRequired;

        @Schema(
                description = "약관 동의 상태 사유. 후보: NONE(필요 없음), INITIAL_REQUIRED_TERMS(최초 필수 약관 동의 필요), UPDATED_REQUIRED_TERMS(개정된 필수 약관 재동의 필요), OPTIONAL_TERMS_AVAILABLE(선택 약관 동의/변경 가능)",
                example = "INITIAL_REQUIRED_TERMS",
                allowableValues = {"NONE", "INITIAL_REQUIRED_TERMS", "UPDATED_REQUIRED_TERMS", "OPTIONAL_TERMS_AVAILABLE"}
        )
        private String reason;

        @Schema(
                description = "프론트가 POST /api/v1/terms/agreements 호출 시 context로 사용할 값. 후보: NONE, SIGNUP, RECONSENT, OPTIONAL_UPDATE",
                example = "SIGNUP",
                allowableValues = {"NONE", "SIGNUP", "RECONSENT", "OPTIONAL_UPDATE"}
        )
        private String submitContext;

        @Schema(description = "현재 사용자 기준 약관 항목 목록")
        private List<TermItemResponse> terms;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "약관 항목 응답")
    public static class TermItemResponse {

        @Schema(
                description = "약관 코드. 초기 후보: SERVICE_TERMS(서비스 이용약관), PRIVACY_POLICY(개인정보 처리방침), MARKETING(마케팅 정보 수신 동의)",
                example = "SERVICE_TERMS",
                allowableValues = {"SERVICE_TERMS", "PRIVACY_POLICY", "MARKETING"}
        )
        private String termCode;

        @Schema(description = "약관 제목", example = "서비스 이용약관")
        private String title;

        @Schema(description = "필수 약관 여부. true면 동의해야 서비스 이용 가능", example = "true")
        private Boolean required;

        @Schema(description = "현재 최신 약관 버전 ID", example = "1")
        private Long currentTermVersionId;

        @Schema(description = "현재 최신 약관 버전", example = "1.0")
        private String currentVersion;

        @Schema(description = "사용자가 이전에 결정한 약관 버전. 최초 동의 전이면 null", example = "1.0", nullable = true)
        private String previousVersion;

        @Schema(description = "현재 최신 버전에 동의한 상태인지 여부. true면 해당 currentTermVersionId에 이미 동의한 상태", example = "false")
        private Boolean agreed;

        @Schema(description = "이번 화면에서 사용자에게 새 동의/거절 결정을 받아야 하는지 여부", example = "true")
        private Boolean needsAgreement;

        @Schema(description = "사용자 이전 결정 버전과 현재 최신 버전이 다른지 여부", example = "false")
        private Boolean updated;

        @Schema(description = "현재 최신 버전이 재동의를 요구하는지 여부", example = "true")
        private Boolean requiresReconsent;

        @Schema(description = "동의 전 서비스 진입 차단 여부. true면 해당 약관 동의 전 서비스 진입 차단", example = "true")
        private Boolean blocking;

        @Schema(
                description = "프론트 화면 분기용 서버 계산 액션. 후보: NONE(처리 없음), INITIAL_AGREEMENT_REQUIRED(최초 동의 필요), RECONSENT_REQUIRED(필수 약관 재동의 필요), OPTIONAL_CONSENT_AVAILABLE(선택 약관 동의 가능), OPTIONAL_RECONSENT_AVAILABLE(선택 약관 재동의 가능)",
                example = "INITIAL_AGREEMENT_REQUIRED",
                allowableValues = {"NONE", "INITIAL_AGREEMENT_REQUIRED", "RECONSENT_REQUIRED", "OPTIONAL_CONSENT_AVAILABLE", "OPTIONAL_RECONSENT_AVAILABLE"}
        )
        private String action;

        @Schema(description = "약관 본문 상세 조회 URL", example = "/api/v1/terms/versions/1", nullable = true)
        private String contentUrl;

        @Schema(description = "약관 본문. agreement-screen 응답에서만 포함", nullable = true)
        private String content;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "약관 버전 상세 응답")
    public static class TermVersionDetailResponse {

        @Schema(
                description = "약관 코드. 초기 후보: SERVICE_TERMS(서비스 이용약관), PRIVACY_POLICY(개인정보 처리방침), MARKETING(마케팅 정보 수신 동의)",
                example = "SERVICE_TERMS",
                allowableValues = {"SERVICE_TERMS", "PRIVACY_POLICY", "MARKETING"}
        )
        private String termCode;

        @Schema(description = "약관 제목", example = "서비스 이용약관")
        private String title;

        @Schema(description = "필수 약관 여부. true면 동의해야 서비스 이용 가능", example = "true")
        private Boolean required;

        @Schema(description = "약관 버전 ID", example = "1")
        private Long termVersionId;

        @Schema(description = "약관 버전", example = "1.0")
        private String version;

        @Schema(description = "약관 효력 발생 시각")
        private LocalDateTime effectiveFrom;

        @Schema(
                description = "약관 본문 형식. 후보: TEXT(일반 텍스트), HTML(HTML), MARKDOWN(마크다운)",
                example = "TEXT",
                allowableValues = {"TEXT", "HTML", "MARKDOWN"}
        )
        private String contentFormat;

        @Schema(description = "약관 본문")
        private String content;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "약관 동의 제출 응답")
    public static class SubmitAgreementsResponse {

        @Schema(description = "서비스 진입 전 약관 동의 화면이 필요한지 여부. false면 필수 약관 기준 서비스 진입 가능", example = "false")
        private Boolean agreementRequired;

        @Schema(
                description = "약관 동의 상태 사유. 후보: NONE(필요 없음), INITIAL_REQUIRED_TERMS(최초 필수 약관 동의 필요), UPDATED_REQUIRED_TERMS(개정된 필수 약관 재동의 필요), OPTIONAL_TERMS_AVAILABLE(선택 약관 동의/변경 가능)",
                example = "NONE",
                allowableValues = {"NONE", "INITIAL_REQUIRED_TERMS", "UPDATED_REQUIRED_TERMS", "OPTIONAL_TERMS_AVAILABLE"}
        )
        private String reason;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "약관 동의 이력 삭제 응답")
    public static class DeleteAgreementHistoryResponse {

        @Schema(description = "삭제된 사용자 약관 동의 이력 수", example = "3")
        private Long deletedCount;

        @Schema(description = "삭제 후 서비스 진입 전 약관 동의 화면이 필요한지 여부", example = "true")
        private Boolean agreementRequired;

        @Schema(
                description = "삭제 후 약관 동의 상태 사유. 후보: NONE(필요 없음), INITIAL_REQUIRED_TERMS(최초 필수 약관 동의 필요), UPDATED_REQUIRED_TERMS(개정된 필수 약관 재동의 필요), OPTIONAL_TERMS_AVAILABLE(선택 약관 동의/변경 가능)",
                example = "INITIAL_REQUIRED_TERMS",
                allowableValues = {"NONE", "INITIAL_REQUIRED_TERMS", "UPDATED_REQUIRED_TERMS", "OPTIONAL_TERMS_AVAILABLE"}
        )
        private String reason;
    }
}
