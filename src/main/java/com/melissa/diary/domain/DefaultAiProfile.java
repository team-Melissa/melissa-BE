package com.melissa.diary.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "default_ai_profile")
public class DefaultAiProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = true)
    private String promptText;

    @Column(nullable = true)
    private String profileName;

    @Column(nullable = false)
    private String firstChat;

    @Column(length = 255)
    private String imageS3;

    @Column(length = 30)
    private String hashTag1;
    @Column(length = 30)
    private String hashTag2;

    @Column(length = 255)
    private String feature1;
    @Column(length = 255)
    private String feature2;
    @Column(length = 255)
    private String feature3;

    @Column(length = 255)
    private String q1;
    @Column(length = 255)
    private String q2;
    @Column(length = 255)
    private String q3;
    @Column(length = 255)
    private String q4;
    @Column(length = 255)
    private String q5;
    @Column(length = 255)
    private String q6;

    @CreationTimestamp
    private LocalDateTime createdAt;
} 