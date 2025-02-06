package com.melissa.diary.domain;

import com.melissa.diary.domain.common.BaseEntity;
import com.melissa.diary.domain.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyChatLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(columnDefinition = "TEXT")
    private String content;

    @CreatedDate
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "thread_id", nullable = true)
    private Thread thread;

    @ManyToOne
    @JoinColumn(name = "ai_profile_id", nullable = true)
    private AiProfile aiProfile;
}
