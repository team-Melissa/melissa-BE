package com.melissa.diary.web.dto;

import com.melissa.diary.domain.enums.AgreementContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

public class TermRequestDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "약관 동의 제출 요청")
    public static class SubmitAgreementsRequest {

        @NotNull
        @Schema(description = "약관 동의 제출 맥락", example = "SIGNUP", requiredMode = Schema.RequiredMode.REQUIRED)
        private AgreementContext context;

        @Valid
        @NotEmpty
        @Schema(description = "약관 버전별 동의/거절 결정 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        private List<AgreementDecisionRequest> agreements;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "약관 버전별 동의/거절 결정")
    public static class AgreementDecisionRequest {

        @NotNull
        @Schema(description = "동의 대상 약관 버전 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long termVersionId;

        @NotNull
        @Schema(description = "동의 여부. 선택 약관은 false 제출 가능", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        private Boolean agreed;
    }
}
