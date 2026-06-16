package com.melissa.diary.service;

import com.melissa.diary.apiPayload.code.status.ErrorStatus;
import com.melissa.diary.apiPayload.exception.handler.ErrorHandler;
import com.melissa.diary.converter.TermConverter;
import com.melissa.diary.domain.Term;
import com.melissa.diary.domain.TermVersion;
import com.melissa.diary.domain.User;
import com.melissa.diary.domain.UserTermAgreement;
import com.melissa.diary.domain.enums.AgreementContext;
import com.melissa.diary.domain.enums.AgreementStatusReason;
import com.melissa.diary.domain.enums.TermAgreementAction;
import com.melissa.diary.domain.enums.TermVersionStatus;
import com.melissa.diary.repository.TermRepository;
import com.melissa.diary.repository.TermVersionRepository;
import com.melissa.diary.repository.UserRepository;
import com.melissa.diary.repository.UserTermAgreementRepository;
import com.melissa.diary.web.dto.TermRequestDTO;
import com.melissa.diary.web.dto.TermResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TermAgreementService {

    private final UserRepository userRepository;
    private final TermRepository termRepository;
    private final TermVersionRepository termVersionRepository;
    private final UserTermAgreementRepository userTermAgreementRepository;

    @Transactional(readOnly = true)
    public TermResponseDTO.AgreementStatusResponse getAgreementStatus(Long userId) {
        findUser(userId);
        List<ComputedTermStatus> statuses = loadTermStatuses(userId, LocalDateTime.now());
        return toAgreementStatusResponse(statuses, false);
    }

    @Transactional(readOnly = true)
    public TermResponseDTO.AgreementStatusResponse getAgreementScreen(Long userId) {
        findUser(userId);
        List<ComputedTermStatus> statuses = loadTermStatuses(userId, LocalDateTime.now());
        return toAgreementStatusResponse(statuses, true);
    }

    @Transactional(readOnly = true)
    public TermResponseDTO.TermVersionDetailResponse getTermVersion(Long termVersionId) {
        TermVersion termVersion = termVersionRepository.findByIdWithTerm(termVersionId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.TERM_VERSION_NOT_FOUND));
        validatePublishedAndEffective(termVersion, LocalDateTime.now());
        return TermConverter.toTermVersionDetailResponse(termVersion);
    }

    @Transactional
    public TermResponseDTO.SubmitAgreementsResponse submitAgreements(
            Long userId,
            TermRequestDTO.SubmitAgreementsRequest request
    ) {
        User user = findUser(userId);
        LocalDateTime now = LocalDateTime.now();
        List<ComputedTermStatus> currentStatuses = loadTermStatuses(userId, now);

        validateRequiredAgreements(currentStatuses, request);

        Map<Long, ComputedTermStatus> statusByTermId = currentStatuses.stream()
                .collect(Collectors.toMap(status -> status.currentVersion().getTerm().getId(), Function.identity()));

        for (TermRequestDTO.AgreementDecisionRequest decision : request.getAgreements()) {
            TermVersion requestedVersion = termVersionRepository.findByIdWithTerm(decision.getTermVersionId())
                    .orElseThrow(() -> new ErrorHandler(ErrorStatus.TERM_VERSION_NOT_FOUND));
            validatePublishedAndEffective(requestedVersion, now);

            ComputedTermStatus termStatus = statusByTermId.get(requestedVersion.getTerm().getId());
            if (termStatus == null || !sameId(termStatus.currentVersion().getId(), requestedVersion.getId())) {
                throw new ErrorHandler(ErrorStatus.TERM_VERSION_NOT_CURRENT);
            }

            if (Boolean.TRUE.equals(requestedVersion.getRequired()) && !Boolean.TRUE.equals(decision.getAgreed())) {
                throw new ErrorHandler(ErrorStatus.TERM_REQUIRED_AGREEMENT_REJECTED);
            }

            if (isNoop(termStatus.latestAgreement(), requestedVersion, decision.getAgreed())) {
                continue;
            }

            UserTermAgreement agreement = TermConverter.toUserTermAgreement(
                    user,
                    requestedVersion,
                    decision.getAgreed(),
                    request.getContext(),
                    now
            );
            userTermAgreementRepository.save(agreement);
        }

        List<ComputedTermStatus> updatedStatuses = loadTermStatuses(userId, LocalDateTime.now());
        AgreementStatusReason reason = calculateReason(updatedStatuses);
        return TermResponseDTO.SubmitAgreementsResponse.builder()
                .agreementRequired(hasBlockingTerm(updatedStatuses))
                .reason(reason.name())
                .build();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ErrorHandler(ErrorStatus.USER_NOT_FOUND));
    }

    private List<ComputedTermStatus> loadTermStatuses(Long userId, LocalDateTime now) {
        List<Term> terms = termRepository.findByActiveTrueOrderByDisplayOrderAsc();
        if (terms.isEmpty()) {
            return List.of();
        }

        List<Long> termIds = terms.stream()
                .map(Term::getId)
                .toList();

        Map<Long, TermVersion> currentVersionByTermId = termVersionRepository
                .findByTermIdInAndStatusAndEffectiveFromLessThanEqual(termIds, TermVersionStatus.PUBLISHED, now)
                .stream()
                .collect(Collectors.toMap(
                        termVersion -> termVersion.getTerm().getId(),
                        Function.identity(),
                        this::latestTermVersion
                ));

        Map<Long, UserTermAgreement> latestAgreementByTermId = userTermAgreementRepository
                .findByUserIdAndTermIdIn(userId, termIds)
                .stream()
                .collect(Collectors.toMap(
                        agreement -> agreement.getTerm().getId(),
                        Function.identity(),
                        this::latestAgreement
                ));

        return terms.stream()
                .map(term -> {
                    TermVersion currentVersion = currentVersionByTermId.get(term.getId());
                    if (currentVersion == null) {
                        throw new ErrorHandler(ErrorStatus.TERM_VERSION_NOT_FOUND);
                    }
                    return computeStatus(currentVersion, latestAgreementByTermId.get(term.getId()));
                })
                .toList();
    }

    private ComputedTermStatus computeStatus(TermVersion currentVersion, UserTermAgreement latestAgreement) {
        boolean required = Boolean.TRUE.equals(currentVersion.getRequired());
        boolean requiresReconsent = Boolean.TRUE.equals(currentVersion.getRequiresReconsent());
        boolean latestAgreed = latestAgreement != null && Boolean.TRUE.equals(latestAgreement.getAgreed());
        boolean agreed = latestAgreement != null
                && sameId(latestAgreement.getTermVersion().getId(), currentVersion.getId())
                && latestAgreed;
        boolean updated = latestAgreement != null
                && !sameId(latestAgreement.getTermVersion().getId(), currentVersion.getId());
        boolean blocking = required
                && !agreed
                && (latestAgreement == null || !latestAgreed || requiresReconsent);

        TermAgreementAction action = calculateAction(
                required,
                requiresReconsent,
                latestAgreement,
                latestAgreed,
                agreed,
                updated
        );
        boolean needsAgreement = blocking || action != TermAgreementAction.NONE;

        return new ComputedTermStatus(
                currentVersion,
                latestAgreement,
                agreed,
                needsAgreement,
                updated,
                blocking,
                action
        );
    }

    private TermAgreementAction calculateAction(
            boolean required,
            boolean requiresReconsent,
            UserTermAgreement latestAgreement,
            boolean latestAgreed,
            boolean agreed,
            boolean updated
    ) {
        if (agreed) {
            return TermAgreementAction.NONE;
        }
        if (latestAgreement == null) {
            return required
                    ? TermAgreementAction.INITIAL_AGREEMENT_REQUIRED
                    : TermAgreementAction.OPTIONAL_CONSENT_AVAILABLE;
        }
        if (updated && required && requiresReconsent) {
            return TermAgreementAction.RECONSENT_REQUIRED;
        }
        if (updated && !required && requiresReconsent) {
            return latestAgreed
                    ? TermAgreementAction.OPTIONAL_RECONSENT_AVAILABLE
                    : TermAgreementAction.OPTIONAL_CONSENT_AVAILABLE;
        }
        if (required && !latestAgreed) {
            return TermAgreementAction.INITIAL_AGREEMENT_REQUIRED;
        }
        return TermAgreementAction.NONE;
    }

    private TermResponseDTO.AgreementStatusResponse toAgreementStatusResponse(
            List<ComputedTermStatus> statuses,
            boolean includeContent
    ) {
        AgreementStatusReason reason = calculateReason(statuses);
        AgreementContext submitContext = calculateSubmitContext(reason);

        return TermResponseDTO.AgreementStatusResponse.builder()
                .agreementRequired(hasBlockingTerm(statuses))
                .reason(reason.name())
                .submitContext(submitContext.name())
                .terms(statuses.stream()
                        .map(status -> TermConverter.toTermItemResponse(
                                status.currentVersion(),
                                status.latestAgreement(),
                                status.agreed(),
                                status.needsAgreement(),
                                status.updated(),
                                status.blocking(),
                                status.action(),
                                includeContent
                        ))
                        .toList())
                .build();
    }

    private void validateRequiredAgreements(
            List<ComputedTermStatus> statuses,
            TermRequestDTO.SubmitAgreementsRequest request
    ) {
        Set<Long> agreedVersionIds = request.getAgreements().stream()
                .filter(decision -> Boolean.TRUE.equals(decision.getAgreed()))
                .map(TermRequestDTO.AgreementDecisionRequest::getTermVersionId)
                .collect(Collectors.toSet());

        boolean missingRequiredAgreement = statuses.stream()
                .filter(ComputedTermStatus::blocking)
                .map(status -> status.currentVersion().getId())
                .anyMatch(versionId -> !agreedVersionIds.contains(versionId));

        if (missingRequiredAgreement) {
            throw new ErrorHandler(ErrorStatus.TERM_REQUIRED_AGREEMENT_MISSING);
        }
    }

    private void validatePublishedAndEffective(TermVersion termVersion, LocalDateTime now) {
        if (termVersion.getStatus() != TermVersionStatus.PUBLISHED
                || termVersion.getEffectiveFrom() == null
                || termVersion.getEffectiveFrom().isAfter(now)) {
            throw new ErrorHandler(ErrorStatus.TERM_VERSION_NOT_PUBLISHED);
        }
    }

    private boolean isNoop(UserTermAgreement latestAgreement, TermVersion requestedVersion, Boolean agreed) {
        return latestAgreement != null
                && sameId(latestAgreement.getTermVersion().getId(), requestedVersion.getId())
                && Objects.equals(latestAgreement.getAgreed(), agreed);
    }

    private boolean hasBlockingTerm(List<ComputedTermStatus> statuses) {
        return statuses.stream().anyMatch(ComputedTermStatus::blocking);
    }

    private AgreementStatusReason calculateReason(List<ComputedTermStatus> statuses) {
        if (hasBlockingTerm(statuses)) {
            boolean initialRequired = statuses.stream()
                    .filter(ComputedTermStatus::blocking)
                    .anyMatch(status -> status.action() == TermAgreementAction.INITIAL_AGREEMENT_REQUIRED);
            return initialRequired
                    ? AgreementStatusReason.INITIAL_REQUIRED_TERMS
                    : AgreementStatusReason.UPDATED_REQUIRED_TERMS;
        }

        boolean optionalAvailable = statuses.stream()
                .anyMatch(status -> status.action() == TermAgreementAction.OPTIONAL_CONSENT_AVAILABLE
                        || status.action() == TermAgreementAction.OPTIONAL_RECONSENT_AVAILABLE);
        return optionalAvailable
                ? AgreementStatusReason.OPTIONAL_TERMS_AVAILABLE
                : AgreementStatusReason.NONE;
    }

    private AgreementContext calculateSubmitContext(AgreementStatusReason reason) {
        return switch (reason) {
            case INITIAL_REQUIRED_TERMS -> AgreementContext.SIGNUP;
            case UPDATED_REQUIRED_TERMS -> AgreementContext.RECONSENT;
            case OPTIONAL_TERMS_AVAILABLE -> AgreementContext.OPTIONAL_UPDATE;
            case NONE -> AgreementContext.NONE;
        };
    }

    private TermVersion latestTermVersion(TermVersion left, TermVersion right) {
        return termVersionComparator().compare(left, right) >= 0 ? left : right;
    }

    private UserTermAgreement latestAgreement(UserTermAgreement left, UserTermAgreement right) {
        return agreementComparator().compare(left, right) >= 0 ? left : right;
    }

    private Comparator<TermVersion> termVersionComparator() {
        return Comparator
                .comparing(TermVersion::getEffectiveFrom, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TermVersion::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private Comparator<UserTermAgreement> agreementComparator() {
        return Comparator
                .comparing(UserTermAgreement::getDecidedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(UserTermAgreement::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private boolean sameId(Long left, Long right) {
        return Objects.equals(left, right);
    }

    private record ComputedTermStatus(
            TermVersion currentVersion,
            UserTermAgreement latestAgreement,
            Boolean agreed,
            Boolean needsAgreement,
            Boolean updated,
            Boolean blocking,
            TermAgreementAction action
    ) {
    }
}
