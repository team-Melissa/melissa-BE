package com.melissa.diary.domain;

import com.melissa.diary.domain.common.BaseEntity;
import com.melissa.diary.domain.enums.TermCategory;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "term",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_term_code", columnNames = "term_code")
        },
        indexes = {
                @Index(name = "idx_term_active_order", columnList = "active,display_order")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Term extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "term_code", nullable = false, length = 50)
    private String termCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TermCategory category;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;
}
