package com.melissa.diary.service;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 소셜 로그인(구글/카카오/애플) 토큰 검증 및 유저정보 조회
 */
@Service
@Slf4j
public class SocialAuthService {


    // Google (ID Token 검증)
    public GooglePayload verifyGoogleToken(String idToken) {
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
        log.info(idToken);
        try {
            RestTemplate restTemplate = new RestTemplate();
            GoogleIdTokenResponse response =
                    restTemplate.getForObject(url, GoogleIdTokenResponse.class);

            if (response != null && response.getSub() != null) {
                // TODO: aud(클라이언트ID), iss, email_verified 등 추가 검증
                // 프로토타입이라 생략!
                return GooglePayload.builder()
                        .sub(response.getSub())
                        .email(response.getEmail())
                        .name(response.getName())
                        .build();
            }
        } catch (Exception e) {
            log.error("Google Token 검증 실패", e);
        }
        return null;
    }


    // Kakao (accessToken 검증)
    public KakaoPayload verifyKakaoToken(String accessToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<?> req = new HttpEntity<>(headers);

            ResponseEntity<KakaoUserResponse> resp = restTemplate.exchange(
                    "https://kapi.kakao.com/v2/user/me",
                    HttpMethod.GET,
                    req,
                    KakaoUserResponse.class
            );
            if (resp.getStatusCode() == HttpStatus.OK) {
                KakaoUserResponse body = resp.getBody();
                if (body != null && body.getId() != null) {
                    // email, profile, etc
                    String email = body.getKakao_account().getEmail();
                    String nickname = body.getKakao_account().getProfile().getNickname();
                    return KakaoPayload.builder()
                            .id(body.getId().toString())
                            .email(email)
                            .nickname(nickname)
                            .build();
                }
            }
        } catch (Exception e) {
            log.error("Kakao Token 검증 실패", e);
        }
        return null;
    }


    // Apple (ID Token 검증)
    // Apple Developer Program 등록 이후 개발 진행
    /*
    public ApplePayload verifyAppleToken(String idToken) {
        // (1) https://appleid.apple.com/auth/keys 에서 JWKS라는 걸 가져오기
        // (2) idToken의 header.kid 등과 매칭하기
        // (3) aud(클라이언트ID), iss, exp, sub 등 검증
        return null;
    }
    */


    // 내부 DTOs for Google, Kakao, Apple

    // Google
    @Getter
    @Setter
    public static class GoogleIdTokenResponse {
        private String sub;
        private String email;
        private String name;
        private String aud;
        private String iss;
        private Boolean email_verified;
    }

    @Builder
    @Getter
    public static class GooglePayload {
        private String sub;
        private String email;
        private String name;
    }

    // Kakao
    @Getter
    @Setter
    public static class KakaoUserResponse {
        private Long id;
        private KakaoAccount kakao_account;

        @Getter
        @Setter
        public static class KakaoAccount {
            private String email;
            private Profile profile;
        }
        @Getter
        @Setter
        public static class Profile {
            private String nickname;
        }
    }

    @Builder
    @Getter
    public static class KakaoPayload {
        private String id;
        private String email;
        private String nickname;
    }


    // Apple
    /*
    @Builder
    @Getter
    public static class ApplePayload {
        private String sub;
        private String email;  // null 가능
        private String name;   // null 가능
    }
    */
}