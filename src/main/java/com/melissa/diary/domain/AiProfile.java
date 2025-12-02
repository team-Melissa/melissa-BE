package com.melissa.diary.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * v1.3.0: AI 프로필 (전역 공유 리소스)
 * - ID 1~5: 서버 기본 제공 프로필 (모든 사용자 공유)
 * - 커스텀 프로필 생성 불가
 */
@Entity
@Table(name = "ai_profile")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 프롬프트 전문(또는 생성된 요약) 저장
    @Column(columnDefinition = "TEXT", nullable = true)
    private String promptText;

    // 예: "행복한 빵빵이"
    @Column(nullable = true)
    private String profileName;

    @Column(nullable = false)
    private String firstChat;

    // 예: S3 경로
    @Column(length = 255)
    private String imageS3;

    // 해시태그 최대 2개
    @Column(length = 30)
    private String hashTag1;
    @Column(length = 30)
    private String hashTag2;

    // 특징(Feature) 최대 3개
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

    // 생성 시각
    @CreationTimestamp
    private LocalDateTime createdAt;

    // Soft delete용
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

}
