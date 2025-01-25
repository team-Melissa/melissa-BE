package com.melissa.diary.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

public class UserRequestDTO {
    @Getter
    @Setter
    public static class GoogleOAuthDTO {
        @NotBlank
        private String idToken;
    }


    @Getter
    @Setter
    public static class KakaoOAuthDTO {
        @NotBlank
        private String accessToken;
    }


   
    @Getter
    @Setter
    public static class AppleOAuthDTO {
        @NotBlank
        private String idToken;
    }
    

    @Getter
    @Setter
    public static class RefreshRequestDTO {
        @NotBlank
        private String refreshToken;
    }

}
