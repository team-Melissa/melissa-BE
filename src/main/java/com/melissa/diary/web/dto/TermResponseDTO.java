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

        @Schema(description = "서비스 진입 전 약관 동의 화면이 필요한지 여부", example = "true")
        private Boolean agreementRequired;

        @Schema(description = "약관 동의 상태 사유", example = "INITIAL_REQUIRED_TERMS")
        private String reason;

        @Schema(description = "프론트가 동의 제출 시 사용할 맥락", example = "SIGNUP")
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

        @Schema(description = "약관 코드", example = "SERVICE_TERMS")
        private String termCode;

        @Schema(description = "약관 제목", example = "서비스 이용약관")
        private String title;

        @Schema(description = "필수 약관 여부", example = "true")
        private Boolean required;

        @Schema(description = "현재 최신 약관 버전 ID", example = "1")
        private Long currentTermVersionId;

        @Schema(description = "현재 최신 약관 버전", example = "1.0")
        private String currentVersion;

        @Schema(description = "사용자가 이전에 결정한 약관 버전. 최초 동의 전이면 null", example = "1.0", nullable = true)
        private String previousVersion;

        @Schema(description = "현재 최신 버전에 동의한 상태인지 여부", example = "false")
        private Boolean agreed;

        @Schema(description = "이번 화면에서 새 결정을 받아야 하는지 여부", example = "true")
        private Boolean needsAgreement;

        @Schema(description = "사용자 이전 결정 버전과 현재 최신 버전이 다른지 여부", example = "false")
        private Boolean updated;

        @Schema(description = "현재 최신 버전이 재동의를 요구하는지 여부", example = "true")
        private Boolean requiresReconsent;

        @Schema(description = "동의 전 서비스 진입 차단 여부", example = "true")
        private Boolean blocking;

        @Schema(description = "프론트 화면 분기용 서버 계산 액션", example = "INITIAL_AGREEMENT_REQUIRED")
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

        @Schema(description = "약관 코드", example = "SERVICE_TERMS")
        private String termCode;

        @Schema(description = "약관 제목", example = "서비스 이용약관")
        private String title;

        @Schema(description = "필수 약관 여부", example = "true")
        private Boolean required;

        @Schema(description = "약관 버전 ID", example = "1")
        private Long termVersionId;

        @Schema(description = "약관 버전", example = "1.0")
        private String version;

        @Schema(description = "약관 효력 발생 시각")
        private LocalDateTime effectiveFrom;

        @Schema(description = "약관 본문 형식", example = "TEXT")
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

        @Schema(description = "서비스 진입 전 약관 동의 화면이 필요한지 여부", example = "false")
        private Boolean agreementRequired;

        @Schema(description = "약관 동의 상태 사유", example = "NONE")
        private String reason;
    }
}
