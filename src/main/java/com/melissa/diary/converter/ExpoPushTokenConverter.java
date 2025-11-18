package com.melissa.diary.converter;

import com.melissa.diary.domain.ExpoPushToken;
import com.melissa.diary.domain.User;
import com.melissa.diary.web.dto.ExpoPushTokenRequestDTO;
import com.melissa.diary.web.dto.ExpoPushTokenResponseDTO;

public class ExpoPushTokenConverter {
    
    /**
     * Request DTO → Entity 변환
     */
    public static ExpoPushToken toEntity(
            ExpoPushTokenRequestDTO.RegisterTokenRequest request, 
            User user) {
        return ExpoPushToken.builder()
                .expoPushToken(request.getExpoPushToken())
                .platform(request.getPlatform())
                .deviceId(request.getDeviceId())
                .user(user)
                .invalid(false)
                .build();
    }
    
    /**
     * Entity → Response DTO 변환
     */
    public static ExpoPushTokenResponseDTO.TokenResponse toResponse(ExpoPushToken entity) {
        return ExpoPushTokenResponseDTO.TokenResponse.builder()
                .id(entity.getId())
                .expoPushToken(entity.getExpoPushToken())
                .platform(entity.getPlatform())
                .deviceId(entity.getDeviceId())
                .invalid(entity.getInvalid())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    
    /**
     * 삭제 응답 DTO 생성
     */
    public static ExpoPushTokenResponseDTO.DeleteResponse toDeleteResponse(String expoPushToken) {
        return ExpoPushTokenResponseDTO.DeleteResponse.builder()
                .expoPushToken(expoPushToken)
                .message("Expo Push Token이 삭제되었습니다.")
                .build();
    }
}

