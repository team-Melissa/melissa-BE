package com.melissa.diary.domain;

import com.melissa.diary.domain.enums.Platform;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "expo_push_token", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"expo_push_token"}) // 토큰 유니크 제약
       },
       indexes = {
           @Index(name = "idx_user_id", columnList = "user_id"),
           @Index(name = "idx_expo_push_token", columnList = "expo_push_token")
       })
@EntityListeners(AuditingEntityListener.class)
public class ExpoPushToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Platform platform;
    
    @Column(name = "device_id", length = 255)
    private String deviceId;
    
    @Column(name = "expo_push_token", nullable = false, unique = true, length = 255)
    private String expoPushToken;
    
    @Builder.Default
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean invalid = false;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void markValid() {
        this.invalid = false;
    }

    public void markInvalid() {
        this.invalid = true;
    }
}

