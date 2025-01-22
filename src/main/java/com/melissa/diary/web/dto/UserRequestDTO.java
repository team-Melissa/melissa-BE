package com.melissa.diary.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

public class UserRequestDTO {
    @Getter
    @Setter
    public static class GoogleOAuthDto {
        @NotBlank
        private String idToken;
    }


    @Getter
    @Setter
    public static class KakaoOAuthDto {
        @NotBlank
        private String accessToken;
    }


   
    @Getter
    @Setter
    public static class AppleOAuthDto {
        @NotBlank
        private String idToken;
    }
    

    @Getter
    @Setter
    public static class RefreshRequestDto {
        @NotBlank
        private String refreshToken;
    }

}
