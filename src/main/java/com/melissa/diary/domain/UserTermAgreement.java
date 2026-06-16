package com.melissa.diary.domain;

import com.melissa.diary.domain.common.BaseEntity;
import com.melissa.diary.domain.enums.AgreementContext;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_term_agreement",
        indexes = {
                @Index(name = "idx_user_term_agreement_user_term_decided", columnList = "user_id,term_id,decided_at,id"),
                @Index(name = "idx_user_term_agreement_user_version", columnList = "user_id,term_version_id"),
                @Index(name = "idx_user_term_agreement_version", columnList = "term_version_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTermAgreement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_version_id", nullable = false)
    private TermVersion termVersion;

    @Column(name = "term_code_snapshot", nullable = false, length = 50)
    private String termCodeSnapshot;

    @Column(name = "version_label_snapshot", nullable = false, length = 20)
    private String versionLabelSnapshot;

    @Column(name = "title_snapshot", nullable = false, length = 100)
    private String titleSnapshot;

    @Column(name = "required_snapshot", nullable = false)
    private Boolean requiredSnapshot;

    @Column(nullable = false)
    private Boolean agreed;

    @Enumerated(EnumType.STRING)
    @Column(name = "agreement_context", nullable = false, length = 30)
    private AgreementContext agreementContext;

    @Column(name = "decided_at", nullable = false)
    private LocalDateTime decidedAt;
}
