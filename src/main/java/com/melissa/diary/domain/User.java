package com.melissa.diary.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

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
@Table(name = "`user`") // 백틱으로 감싸기
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true, length = 100)
    private String email;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(nullable = true, length = 100)
    private String providerId;

    @Column(nullable = false, length = 50)
    private String nickname;

    /**
     * Refresh 토큰 저장 (실제로는 SHA-256 해시값을 저장함)
     * - 보안: 평문 대신 해시를 저장하여 DB 유출 시 토큰 재사용 방지
     * - 마이그레이션: 평문 -> 해시값으로 변경되었지만, 재로그인 강제하여 문제 없음.
     */
    @Column(nullable = true, length = 255)
    private String refreshToken;

    private LocalDateTime refreshTokenExpiry;

    @CreatedDate
    private LocalDateTime createAt;

    @LastModifiedDate
    private LocalDateTime updateAt;

    // ✨ 새 필드
    private Integer dailyQuota;     // 오늘 남은 수량
    private LocalDate quotaDate;    // 마지막 초기화 날짜

    @Version
    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    @Builder.Default
    private Long version = 0L;

    // 마이그레이션용, Initialize를 통해 기존 사용자도 처리위해 추가
    @PrePersist
    public void initQuota() {          // 신규 가입 시
        if (dailyQuota == null) dailyQuota = 100;
        if (quotaDate  == null) quotaDate  = LocalDate.now();
    }

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @Builder.Default
    private List<UserSetting> userSettingList = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Donation> donationList = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Thread> threadList = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Diary> diaryList = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ExpoPushToken> expoPushTokenList = new ArrayList<>();

    // 연관관계 편의 메소드
    public void addUserSetting(UserSetting userSetting) {
        userSettingList.add(userSetting);
        userSetting.setUser(this);
    }

    public void addDonation(Donation donation) {
        donationList.add(donation);
        donation.setUser(this);
    }

    public void addThread(Thread thread) {
        threadList.add(thread);
        thread.setUser(this);
    }

    public void addDiary(Diary diary) {
        diaryList.add(diary);
        diary.setUser(this);
    }

    public void addExpoPushToken(ExpoPushToken expoPushToken) {
        expoPushTokenList.add(expoPushToken);
        expoPushToken.setUser(this);
    }

}
