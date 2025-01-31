package com.melissa.diary.web.dto;

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
    public static class GoogleOAuthDTO {
        @NotBlank
        private String idToken;
    }


    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KakaoOAuthDTO {
        @NotBlank
        private String accessToken;
    }


   
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppleOAuthDTO {
        @NotBlank
        private String idToken;
    }
    

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshRequestDTO {
        @NotBlank
        private String refreshToken;
    }

}
