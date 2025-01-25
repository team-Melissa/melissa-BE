package com.melissa.diary.domain;

import jakarta.persistence.*;
import lombok.*;

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
    private String image;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Column(nullable = true, length = 100)
    private String sampleQuestionAnswer;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String personalitySummary;

    @Column(nullable = true, length = 30)
    private String hashtags;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true)
    private User user;



}
