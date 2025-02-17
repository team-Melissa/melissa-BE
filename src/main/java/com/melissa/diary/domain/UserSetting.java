package com.melissa.diary.domain;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Time;

@Getter
@Builder
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user_setting", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id"}) // 유저당 하나만 존재하도록 유니크 제약 조건 추가
})
public class UserSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private boolean notificationSummary;

    private boolean notificationQna;

    private Time sleepTime;

    private Time notificationTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
