package com.melissa.diary.domain;

import com.melissa.diary.domain.enums.Mood;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "`thread`")
public class Thread {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true, length = 255)
    private String gptThreadId;

    @Column(nullable = true, length = 255)
    private String assistantId;

    @CreatedDate
    private LocalDate threadDate;

    // 아래부턴 요약필요라서 null 가능
    @Column(nullable = true, length = 30)
    private String summaryTitle;

    @Enumerated(EnumType.STRING)
    private Mood mood;

    @Column(nullable = true, length = 255)
    private String moodImage;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String summaryContent;

    @Column(nullable = true)
    private LocalDateTime summaryCreatedAt;

    @Column(nullable = true, length = 30)
    private String hashtags;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

}