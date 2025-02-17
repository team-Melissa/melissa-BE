package com.melissa.diary.domain;

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

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "thread",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "year", "month", "day"}) // 유저별 날짜 유니크 조건 추가
        }
)
public class Thread {
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

    // 아래부턴 요약필요라서 null 가능
    @Column(nullable = true, length = 50)
    private String summaryTitle;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private Mood mood;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String summaryContent;

    @Column(nullable = true, length = 30)
    private String hashtag1;

    @Column(nullable = true, length = 30)
    private String hashtag2;

    @Column(nullable = true)
    private String imageUrl;

    @Column(nullable = true)
    private LocalDateTime summaryCreatedAt;

    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL)
    private List<DailyChatLog> dailyChatLogs = new ArrayList<>();

    // 연관관계 편의 메소드
    public void addDailyChatLog(DailyChatLog dailyChatLog) {
        dailyChatLogs.add(dailyChatLog);
        dailyChatLog.setThread(this);
    }

}