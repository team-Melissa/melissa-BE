package com.melissa.diary.domain;

import com.melissa.diary.converter.EncryptionAttributeConverter;
import com.melissa.diary.domain.common.BaseEntity;
import com.melissa.diary.domain.enums.DiaryImageStatus;
import com.melissa.diary.domain.enums.DiaryType;
import com.melissa.diary.domain.enums.Mood;
import jakarta.persistence.*;
import lombok.*;

/**
 * v1.3.0: 일기 버전 관리를 위한 Diary 엔티티
 * Thread에서 분리하여 한 Thread에서 여러 버전의 일기 생성 가능
 */
@Entity
@Table(name = "diary")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Diary extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    private Thread thread;
    
    @Column(nullable = false)
    private int year;
    
    @Column(nullable = false)
    private int month;
    
    @Column(nullable = false)
    private int day;
    
    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptionAttributeConverter.class)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptionAttributeConverter.class)
    private String content;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private Mood mood;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DiaryType type = DiaryType.MANUAL;
    
    @Column(length = 30)
    private String hashtag1;
    
    @Column(length = 30)
    private String hashtag2;
    
    @Column(length = 255)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_status", nullable = false, length = 20)
    @Builder.Default
    private DiaryImageStatus imageStatus = DiaryImageStatus.NONE;
    
    @Column(nullable = false)
    @Builder.Default
    private int version = 1;
    
    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;

    public void deactivate() {
        this.isActive = false;
    }

    public void requestImageGeneration() {
        this.imageStatus = DiaryImageStatus.PENDING;
        this.imageUrl = null;
    }

    public void markImageReady(String imageUrl) {
        this.imageStatus = DiaryImageStatus.READY;
        this.imageUrl = imageUrl;
    }

    public void markImageFailed(String fallbackImageUrl) {
        this.imageStatus = DiaryImageStatus.FAILED;
        this.imageUrl = fallbackImageUrl;
    }
}

