package com.melissa.diary.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
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

    @Column(nullable = true, length = 255)
    private String refreshToken;

    private LocalDateTime refreshTokenExpiry;

    @CreatedDate
    private LocalDateTime createAt;

    @LastModifiedDate
    private LocalDateTime updateAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<UserSetting> userSettingList = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Donation> donationList = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Thread> threadList = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<AiProfile> aiProfileList = new ArrayList<>();

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

    public void addAiProfile(AiProfile aiProfile) {
        aiProfileList.add(aiProfile);
        aiProfile.setUser(this);
    }

}
