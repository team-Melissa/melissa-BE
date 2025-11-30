package com.melissa.diary.domain;

import com.melissa.diary.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
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