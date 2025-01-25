package com.melissa.diary.domain;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Time;

@Getter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class UserSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private boolean notificationSummary;

    private boolean notificationQna;

    private Time sleepTime;

    private Time notificationTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId")
    private User user;
}
