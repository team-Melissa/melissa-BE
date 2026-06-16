package com.melissa.diary.service;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.domain.Term;
import com.melissa.diary.domain.TermVersion;
import com.melissa.diary.domain.User;
import com.melissa.diary.domain.UserTermAgreement;
import com.melissa.diary.domain.enums.AgreementContext;
import com.melissa.diary.domain.enums.TermCategory;
import com.melissa.diary.domain.enums.TermContentFormat;
import com.melissa.diary.domain.enums.TermVersionStatus;
import com.melissa.diary.repository.TermRepository;
import com.melissa.diary.repository.TermVersionRepository;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.repository.UserTermAgreementRepository;
import com.melissa.diary.web.dto.TermRequestDTO;
import com.melissa.diary.web.dto.TermResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TermAgreementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TermRepository termRepository;

    @Mock
    private TermVersionRepository termVersionRepository;

    @Mock
    private UserTermAgreementRepository userTermAgreementRepository;

    private TermAgreementService termAgreementService;

    private User user;
    private Term serviceTerm;
    private Term privacyTerm;
    private Term marketingTerm;
    private TermVersion serviceV1;
    private TermVersion privacyV1;
    private TermVersion marketingV1;

    @BeforeEach
    void setUp() {
        termAgreementService = new TermAgreementService(
                userRepository,
                termRepository,
                termVersionRepository,
                userTermAgreementRepository
        );

        user = User.builder()
                .id(1L)
                .provider("GOOGLE")
                .nickname("user")
                .build();
        serviceTerm = term(1L, "SERVICE_TERMS", TermCategory.SERVICE, 1);
        privacyTerm = term(2L, "PRIVACY_POLICY", TermCategory.PRIVACY, 2);
        marketingTerm = term(3L, "MARKETING", TermCategory.MARKETING, 3);
        serviceV1 = version(101L, serviceTerm, "1.0", "서비스 이용약관", true, true);
        privacyV1 = version(102L, privacyTerm, "1.0", "개인정보 처리방침", true, true);
        marketingV1 = version(103L, marketingTerm, "1.0", "마케팅 정보 수신 동의", false, false);
    }

    @Test
    void getAgreementStatusRequiresInitialRequiredTermsForNewUser() {
        mockCurrentTerms(List.of(serviceV1, privacyV1, marketingV1));
        when(userTermAgreementRepository.findByUserIdAndTermIdIn(eq(user.getId()), anyList()))
                .thenReturn(List.of());

        TermResponseDTO.AgreementStatusResponse response = termAgreementService.getAgreementStatus(user.getId());

        assertThat(response.getAgreementRequired()).isTrue();
        assertThat(response.getReason()).isEqualTo("INITIAL_REQUIRED_TERMS");
        assertThat(response.getSubmitContext()).isEqualTo("SIGNUP");
        assertThat(response.getTerms()).hasSize(3);
        assertThat(response.getTerms().get(0).getAction()).isEqualTo("INITIAL_AGREEMENT_REQUIRED");
        assertThat(response.getTerms().get(0).getBlocking()).isTrue();
        assertThat(response.getTerms().get(1).getBlocking()).isTrue();
        assertThat(response.getTerms().get(2).getAction()).isEqualTo("OPTIONAL_CONSENT_AVAILABLE");
        assertThat(response.getTerms().get(2).getBlocking()).isFalse();
        assertThat(response.getTerms().get(0).getContent()).isNull();
        assertThat(response.getTerms().get(0).getContentUrl()).isEqualTo("/api/v1/terms/versions/101");
    }

    @Test
    void getAgreementStatusRequiresReconsentForUpdatedRequiredTerm() {
        TermVersion serviceV11 = version(111L, serviceTerm, "1.1", "서비스 이용약관", true, true);
        UserTermAgreement serviceAgreementV1 = agreement(1L, serviceV1, true);
        UserTermAgreement privacyAgreementV1 = agreement(2L, privacyV1, true);

        mockCurrentTerms(List.of(serviceV11, privacyV1, marketingV1));
        when(userTermAgreementRepository.findByUserIdAndTermIdIn(eq(user.getId()), anyList()))
                .thenReturn(List.of(serviceAgreementV1, privacyAgreementV1));

        TermResponseDTO.AgreementStatusResponse response = termAgreementService.getAgreementStatus(user.getId());

        assertThat(response.getAgreementRequired()).isTrue();
        assertThat(response.getReason()).isEqualTo("UPDATED_REQUIRED_TERMS");
        assertThat(response.getSubmitContext()).isEqualTo("RECONSENT");
        assertThat(response.getTerms().get(0).getCurrentVersion()).isEqualTo("1.1");
        assertThat(response.getTerms().get(0).getPreviousVersion()).isEqualTo("1.0");
        assertThat(response.getTerms().get(0).getUpdated()).isTrue();
        assertThat(response.getTerms().get(0).getAction()).isEqualTo("RECONSENT_REQUIRED");
        assertThat(response.getTerms().get(0).getBlocking()).isTrue();
    }

    @Test
    void submitAgreementsRejectsMissingBlockingRequiredTerm() {
        mockCurrentTerms(List.of(serviceV1, privacyV1, marketingV1));
        when(userTermAgreementRepository.findByUserIdAndTermIdIn(eq(user.getId()), anyList()))
                .thenReturn(List.of());

        TermRequestDTO.SubmitAgreementsRequest request = new TermRequestDTO.SubmitAgreementsRequest(
                AgreementContext.SIGNUP,
                List.of(new TermRequestDTO.AgreementDecisionRequest(marketingV1.getId(), false))
        );

        ErrorHandler error = assertThrows(
                ErrorHandler.class,
                () -> termAgreementService.submitAgreements(user.getId(), request)
        );

        assertThat(error.getErrorCode()).isEqualTo(ErrorStatus.TERM_REQUIRED_AGREEMENT_MISSING);
    }

    @Test
    void submitAgreementsStoresDecisionsAndReturnsNoBlockingStatus() {
        mockCurrentTerms(List.of(serviceV1, privacyV1, marketingV1));
        when(termVersionRepository.findByIdWithTerm(serviceV1.getId())).thenReturn(Optional.of(serviceV1));
        when(termVersionRepository.findByIdWithTerm(privacyV1.getId())).thenReturn(Optional.of(privacyV1));
        when(termVersionRepository.findByIdWithTerm(marketingV1.getId())).thenReturn(Optional.of(marketingV1));

        UserTermAgreement serviceAgreement = agreement(1L, serviceV1, true);
        UserTermAgreement privacyAgreement = agreement(2L, privacyV1, true);
        UserTermAgreement marketingAgreement = agreement(3L, marketingV1, false);
        when(userTermAgreementRepository.findByUserIdAndTermIdIn(eq(user.getId()), anyList()))
                .thenReturn(List.of())
                .thenReturn(List.of(serviceAgreement, privacyAgreement, marketingAgreement));
        when(userTermAgreementRepository.save(any(UserTermAgreement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TermRequestDTO.SubmitAgreementsRequest request = new TermRequestDTO.SubmitAgreementsRequest(
                AgreementContext.SIGNUP,
                List.of(
                        new TermRequestDTO.AgreementDecisionRequest(serviceV1.getId(), true),
                        new TermRequestDTO.AgreementDecisionRequest(privacyV1.getId(), true),
                        new TermRequestDTO.AgreementDecisionRequest(marketingV1.getId(), false)
                )
        );

        TermResponseDTO.SubmitAgreementsResponse response = termAgreementService.submitAgreements(user.getId(), request);

        assertThat(response.getAgreementRequired()).isFalse();
        assertThat(response.getReason()).isEqualTo("NONE");
        verify(userTermAgreementRepository, times(3)).save(any(UserTermAgreement.class));
    }

    private void mockCurrentTerms(List<TermVersion> versions) {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(termRepository.findByActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(List.of(serviceTerm, privacyTerm, marketingTerm));
        when(termVersionRepository.findByTermIdInAndStatusAndEffectiveFromLessThanEqual(
                anyList(),
                eq(TermVersionStatus.PUBLISHED),
                any(LocalDateTime.class)
        )).thenReturn(versions);
    }

    private Term term(Long id, String termCode, TermCategory category, Integer displayOrder) {
        return Term.builder()
                .id(id)
                .termCode(termCode)
                .category(category)
                .displayOrder(displayOrder)
                .active(true)
                .build();
    }

    private TermVersion version(
            Long id,
            Term term,
            String versionLabel,
            String title,
            Boolean required,
            Boolean requiresReconsent
    ) {
        return TermVersion.builder()
                .id(id)
                .term(term)
                .versionLabel(versionLabel)
                .title(title)
                .required(required)
                .content(title + " " + versionLabel + " 내용")
                .contentFormat(TermContentFormat.TEXT)
                .status(TermVersionStatus.PUBLISHED)
                .requiresReconsent(requiresReconsent)
                .effectiveFrom(LocalDateTime.now().minusDays(1))
                .publishedAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    private UserTermAgreement agreement(Long id, TermVersion termVersion, Boolean agreed) {
        return UserTermAgreement.builder()
                .id(id)
                .user(user)
                .term(termVersion.getTerm())
                .termVersion(termVersion)
                .termCodeSnapshot(termVersion.getTerm().getTermCode())
                .versionLabelSnapshot(termVersion.getVersionLabel())
                .titleSnapshot(termVersion.getTitle())
                .requiredSnapshot(termVersion.getRequired())
                .agreed(agreed)
                .agreementContext(AgreementContext.SIGNUP)
                .decidedAt(LocalDateTime.now().minusHours(1).plusMinutes(id))
                .build();
    }
}
