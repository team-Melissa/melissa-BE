package com.melissa.diary.domain;

import jakarta.persistence.*;
import lombok.*;

@Builder
@Getter
@Entity
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MonthlySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int year;

    private int month;

    private String summaryText;

    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
