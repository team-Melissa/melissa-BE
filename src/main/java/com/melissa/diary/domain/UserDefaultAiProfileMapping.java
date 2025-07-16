package com.melissa.diary.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_default_ai_profile_mapping",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "default_ai_profile_id"}),
    indexes = {@Index(name = "idx_user_active", columnList = "user_id,active")}
)
@Getter
@Setter
@Builder
@AllArgsConstructor
public class UserDefaultAiProfileMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_ai_profile_id")
    private DefaultAiProfile defaultAiProfile;

    private LocalDateTime createdAt;
    private boolean active;

    // JPA 기본 생성자
    protected UserDefaultAiProfileMapping() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
} 