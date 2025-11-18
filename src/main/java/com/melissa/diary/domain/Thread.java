package com.melissa.diary.domain;

import com.melissa.diary.domain.common.BaseEntity;
import com.melissa.diary.domain.enums.Mood;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * v1.3.0: 캐릭터별 대화 Thread 관리
 * - UniqueConstraint: (user_id, ai_profile_id, year, month, day)
 * - 1일 1캐릭터 1Thread (최대 5개)
 * - 일기 관련 컬럼 제거 (Diary 테이블로 분리)
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "thread",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "ai_profile_id", "year", "month", "day"})
        }
)
public class Thread extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

/*    @Column(nullable = false, length = 255)
    private String gptThreadId;

    @Column(nullable = false, length = 255)
    private String assistantId;*/

    @Column(nullable = false)
    private int year;

    @Max(12) @Min(1)
    @Column(nullable = false)
    private int month;

    @Max(31) @Min(1)
    @Column(nullable = false)
    private int day;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "ai_profile_id", nullable = false)
    private AiProfile aiProfile;

    // ========== v1.3.0: Deprecated 필드 (임시 유지, DiaryService 완성 후 제거) ==========
    @Deprecated
    @Column(nullable = true, columnDefinition = "TEXT")
    @Convert(converter = com.melissa.diary.converter.EncryptionAttributeConverter.class)
    private String summaryTitle;

    @Deprecated
    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private Mood mood;

    @Deprecated
    @Column(nullable = true, columnDefinition = "TEXT")
    @Convert(converter = com.melissa.diary.converter.EncryptionAttributeConverter.class)
    private String summaryContent;

    @Deprecated
    @Column(nullable = true, length = 30)
    private String hashtag1;

    @Deprecated
    @Column(nullable = true, length = 30)
    private String hashtag2;

    @Deprecated
    @Column(nullable = true)
    private String imageUrl;

    @Deprecated
    @Column(nullable = true)
    private LocalDateTime summaryCreatedAt;

    @Deprecated
    @Column(nullable = true)
    private LocalDateTime lastSummaryRequestAt;
    // ========== Deprecated 필드 끝 ==========

    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL)
    @Builder.Default
    private List<DailyChatLog> dailyChatLogs = new ArrayList<>();

    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Diary> diaries = new ArrayList<>();

    // 연관관계 편의 메소드
    public void addDailyChatLog(DailyChatLog dailyChatLog) {
        dailyChatLogs.add(dailyChatLog);
        dailyChatLog.setThread(this);
    }

    public void addDiary(Diary diary) {
        diaries.add(diary);
        diary.setThread(this);
    }

}