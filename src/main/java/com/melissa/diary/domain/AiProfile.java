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

    // 생성 시각
    @CreationTimestamp
    private LocalDateTime createdAt;

    // 소유 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;



}
