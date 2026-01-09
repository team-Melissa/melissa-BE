package com.melissa.diary.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

public class UserResponseDTO {
    
    @Getter
    @Builder
    @Schema(description = "OAuth 로그인 결과")
    public static class OAuthLoginResultDTO {
        
        @Schema(description = "사용자 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long userId;
        
        @Schema(description = "OAuth 제공자", example = "GOOGLE", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String oauthProvider;
        
        @Schema(description = "이메일", example = "user@example.com", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String email;
        
        @Schema(description = "닉네임", example = "홍길동", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String nickname;
        
        @Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", requiredMode = Schema.RequiredMode.REQUIRED)
        private String accessToken;
        
        @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", requiredMode = Schema.RequiredMode.REQUIRED)
        private String refreshToken;
        
        @Schema(description = "토큰 타입", example = "Bearer", requiredMode = Schema.RequiredMode.REQUIRED)
        private String tokenType;
    }

    @Getter
    @Builder
    @Schema(description = "리프레시 토큰 응답")
    public static class RefreshTokenResponseDTO {
        
        @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", requiredMode = Schema.RequiredMode.REQUIRED)
        private String refreshToken;
        
        @Schema(description = "토큰 타입", example = "Bearer", requiredMode = Schema.RequiredMode.REQUIRED)
        private String tokenType;
        
        @Schema(description = "만료 시간 (초)", example = "3600", requiredMode = Schema.RequiredMode.REQUIRED)
        private int expireIn;
    }

    @Getter
    @Builder
    @Schema(description = "회원 탈퇴 결과")
    public static class DeleteResultDTO {
        
        @Schema(description = "사용자 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long userId;
        
        @Schema(description = "OAuth 제공자", example = "GOOGLE", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String oauthProvider;
        
        @Schema(description = "제공자 ID", example = "1234567890", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String providerId;
        
        @Schema(description = "이메일", example = "user@example.com", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String email;
        
        @Schema(description = "닉네임", example = "홍길동", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String nickname;
    }

    @Getter
    @Builder
    @Schema(description = "유저 정보 조회 응답")
    public static class MeResponseDTO{

        @Schema(description = "사용자 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long userId;

        @Schema(description = "이메일", example = "user@example.com", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String email;

        @Schema(description = "닉네임", example = "홍길동", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String nickname;

        @Schema(description = "OAuth 제공자", example = "GOOGLE", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        private String oauthProvider;

        @Schema(description = "사용량(쿼터) 정보", requiredMode = Schema.RequiredMode.REQUIRED)
        private UsageDTO usage;

        @Getter
        @Builder
        @Schema(description = "일일 사용량(쿼터) 정보")
        public static class UsageDTO {

            @Schema(description = "오늘 남은 쿼터", example = "97", requiredMode = Schema.RequiredMode.REQUIRED)
            private Integer dailyQuotaRemaining;

            @Schema(description = "일일 쿼터 제한(고정값 - 서버 상수)", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
            private Integer dailyQuotaLimit;

            @Schema(description = "오늘 사용한 쿼터", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
            private Integer dailyQuotaUsed;

            @Schema(description = "사용량 기준 날짜(DB 저장값 그대로)", example = "2026-01-10", requiredMode = Schema.RequiredMode.REQUIRED)
            private java.time.LocalDate quotaDate;
        }

    }
}
