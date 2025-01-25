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

    @Column(nullable = false, length = 100)
    private String profileName;

    @Column(nullable = true, length = 255)
    private String imageUrl;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    // --- 6가지 질문의 결과를 원문으로 저장 ---
    @Column(columnDefinition = "TEXT")
    private String feature1; // 1. 대화 상대 성격
    @Column(columnDefinition = "TEXT")
    private String feature2; // 2. 대화 주제 선호도
    @Column(columnDefinition = "TEXT")
    private String feature3; // 3. 대화 스타일
    @Column(columnDefinition = "TEXT")
    private String feature4; // 4. 연령대 및 분위기
    @Column(columnDefinition = "TEXT")
    private String feature5; // 5. 목적성
    @Column(columnDefinition = "TEXT")
    private String feature6; // 6. 언어 표현 방식

    // --- 해시태그(성격, 연령대, 목적성) ---
    @Column(length = 30)
    private String hashTag1; // 예: 성격 → "#Active"
    @Column(length = 30)
    private String hashTag2; // 예: 연령대 → "#Young"
    @Column(length = 30)
    private String hashTag3; // 예: 목적성 → "#Fun"

    // 생성일시 (자동)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true)
    private User user;



}
