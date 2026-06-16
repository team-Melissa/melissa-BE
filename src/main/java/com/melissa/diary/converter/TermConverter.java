package com.melissa.diary.converter;

import com.melissa.diary.domain.Term;
import com.melissa.diary.domain.TermVersion;
import com.melissa.diary.domain.User;
import com.melissa.diary.domain.UserTermAgreement;
import com.melissa.diary.domain.enums.AgreementContext;
import com.melissa.diary.domain.enums.TermAgreementAction;
import com.melissa.diary.web.dto.TermResponseDTO;

import java.time.LocalDateTime;

public class TermConverter {

    public static TermResponseDTO.TermItemResponse toTermItemResponse(
            TermVersion currentVersion,
            UserTermAgreement latestAgreement,
            Boolean agreed,
            Boolean needsAgreement,
            Boolean updated,
            Boolean blocking,
            TermAgreementAction action,
            boolean includeContent
    ) {
        Term term = currentVersion.getTerm();

        return TermResponseDTO.TermItemResponse.builder()
                .termCode(term.getTermCode())
                .title(currentVersion.getTitle())
                .required(currentVersion.getRequired())
                .currentTermVersionId(currentVersion.getId())
                .currentVersion(currentVersion.getVersionLabel())
                .previousVersion(latestAgreement == null ? null : latestAgreement.getVersionLabelSnapshot())
                .agreed(agreed)
                .needsAgreement(needsAgreement)
                .updated(updated)
                .requiresReconsent(currentVersion.getRequiresReconsent())
                .blocking(blocking)
                .action(action.name())
                .contentUrl(includeContent ? null : "/api/v1/terms/versions/" + currentVersion.getId())
                .content(includeContent ? currentVersion.getContent() : null)
                .build();
    }

    public static TermResponseDTO.TermVersionDetailResponse toTermVersionDetailResponse(TermVersion termVersion) {
        return TermResponseDTO.TermVersionDetailResponse.builder()
                .termCode(termVersion.getTerm().getTermCode())
                .title(termVersion.getTitle())
                .required(termVersion.getRequired())
                .termVersionId(termVersion.getId())
                .version(termVersion.getVersionLabel())
                .effectiveFrom(termVersion.getEffectiveFrom())
                .contentFormat(termVersion.getContentFormat().name())
                .content(termVersion.getContent())
                .build();
    }

    public static UserTermAgreement toUserTermAgreement(
            User user,
            TermVersion termVersion,
            Boolean agreed,
            AgreementContext context,
            LocalDateTime decidedAt
    ) {
        Term term = termVersion.getTerm();

        return UserTermAgreement.builder()
                .user(user)
                .term(term)
                .termVersion(termVersion)
                .termCodeSnapshot(term.getTermCode())
                .versionLabelSnapshot(termVersion.getVersionLabel())
                .titleSnapshot(termVersion.getTitle())
                .requiredSnapshot(termVersion.getRequired())
                .agreed(agreed)
                .agreementContext(context)
                .decidedAt(decidedAt)
                .build();
    }
}
