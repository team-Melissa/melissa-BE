package com.melissa.diary.converter;

import com.melissa.diary.domain.UserMemory;
import com.melissa.diary.web.dto.UserMemoryResponseDTO;

import java.time.LocalDateTime;

public class UserMemoryConverter {
    
    public static UserMemoryResponseDTO.MemoryResponse toMemoryResponse(UserMemory userMemory) {
        if (userMemory == null) {
            return UserMemoryResponseDTO.MemoryResponse.builder()
                    .memoryContent("")
                    .lastUpdatedAt(null)
                    .hasMemory(false)
                    .build();
        }
        
        boolean hasMemory = userMemory.getMemoryContent() != null && 
                           !userMemory.getMemoryContent().trim().isEmpty();
        
        return UserMemoryResponseDTO.MemoryResponse.builder()
                .memoryContent(hasMemory ? userMemory.getMemoryContent() : "")
                .lastUpdatedAt(userMemory.getUpdatedAt())
                .hasMemory(hasMemory)
                .build();
    }
    
    public static UserMemoryResponseDTO.MemoryResetResponse toMemoryResetResponse() {
        return UserMemoryResponseDTO.MemoryResetResponse.builder()
                .message("메모리가 성공적으로 초기화되었습니다.")
                .resetAt(LocalDateTime.now())
                .build();
    }
}
