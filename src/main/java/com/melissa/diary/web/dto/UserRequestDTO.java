package com.melissa.diary.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class UserRequestDTO {
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Google OAuth 로그인 요청")
    public static class GoogleOAuthDTO {
        
        @NotBlank(message = "ID 토큰은 필수입니다.")
        @Schema(description = "Google ID Token", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...", requiredMode = Schema.RequiredMode.REQUIRED)
        private String idToken;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Kakao OAuth 로그인 요청")
    public static class KakaoOAuthDTO {
        
        @NotBlank(message = "액세스 토큰은 필수입니다.")
        @Schema(description = "Kakao Access Token", example = "xxxxxxxxxxxxxxxxxxxxx", requiredMode = Schema.RequiredMode.REQUIRED)
        private String accessToken;
    }
   
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Apple OAuth 로그인 요청")
    public static class AppleOAuthDTO {
        
        @NotBlank(message = "ID 토큰은 필수입니다.")
        @Schema(description = "Apple ID Token", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...", requiredMode = Schema.RequiredMode.REQUIRED)
        private String idToken;
    }
}
