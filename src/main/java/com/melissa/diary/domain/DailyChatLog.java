package com.melissa.diary.domain;

import com.melissa.diary.domain.enums.Role;
import jakarta.persistence.*;
import lombok.*;

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

    @Column(nullable = false) // 단순 채팅밖에 안됨
    private String content;

    @ManyToOne
    @JoinColumn(name = "thread_id", nullable = false)
    private Thread thread;

    @ManyToOne
    @JoinColumn(name = "ai_profile_id", nullable = false)
    private AiProfile aiProfile;
}
