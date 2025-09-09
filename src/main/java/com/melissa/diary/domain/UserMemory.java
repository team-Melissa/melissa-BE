package com.melissa.diary.domain;

import com.melissa.diary.converter.EncryptionAttributeConverter;
import com.melissa.diary.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_memory")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMemory extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    @Column(name = "memory_content", columnDefinition = "TEXT")
    @Convert(converter = EncryptionAttributeConverter.class)
    private String memoryContent;
    
    @Version
    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    @Builder.Default
    private Long version = 0L;
    
    // 메모리 업데이트를 위한 편의 메서드
    public void updateMemoryContent(String newContent) {
        this.memoryContent = newContent;
        // BaseEntity의 updatedAt이 자동으로 설정됨
    }
}
