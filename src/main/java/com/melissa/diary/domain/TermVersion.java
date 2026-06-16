package com.melissa.diary.domain;

import com.melissa.diary.domain.common.BaseEntity;
import com.melissa.diary.domain.enums.TermContentFormat;
import com.melissa.diary.domain.enums.TermVersionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "term_version",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_term_version_term_version", columnNames = {"term_id", "version_label"})
        },
        indexes = {
                @Index(name = "idx_term_version_current", columnList = "term_id,status,effective_from")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TermVersion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @Column(name = "version_label", nullable = false, length = 20)
    private String versionLabel;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private Boolean required;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "content_format", nullable = false, length = 20)
    private TermContentFormat contentFormat = TermContentFormat.TEXT;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private TermVersionStatus status = TermVersionStatus.DRAFT;

    @Builder.Default
    @Column(name = "requires_reconsent", nullable = false)
    private Boolean requiresReconsent = true;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;
}
