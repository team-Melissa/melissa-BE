package com.melissa.diary.service;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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
    public ApplePayload verifyAppleToken(String idToken) {
        try {
            // 1. 애플의 공개키를 가져옵니다.
            String appleKeysUrl = "https://appleid.apple.com/auth/keys";
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(appleKeysUrl, String.class);
            JWKSet jwkSet = JWKSet.parse(response);

            // 2. 전달받은 idToken을 파싱합니다.
            SignedJWT signedJWT = SignedJWT.parse(idToken);
            String keyId = signedJWT.getHeader().getKeyID();

            // 3. token의 header.kid와 일치하는 JWK를 찾습니다.
            JWK jwk = jwkSet.getKeyByKeyId(keyId);
            if (jwk == null) {
                log.error("Apple Token 검증 실패: Matching JWK not found for kid: {}", keyId);
                return null;
            }
            com.nimbusds.jose.jwk.RSAKey rsaKey = (com.nimbusds.jose.jwk.RSAKey) jwk;
            RSAPublicKey publicKey = rsaKey.toRSAPublicKey();

            // 4. JWT 서명을 검증합니다.
            JWSVerifier verifier = new RSASSAVerifier(publicKey);
            if (!signedJWT.verify(verifier)) {
                log.error("Apple Token 검증 실패: Signature verification failed");
                return null;
            }

            // 5. Claims를 파싱하고 검증합니다.
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            // 발급자(issuer) 검증
            if (!"https://appleid.apple.com".equals(claims.getIssuer())) {
                log.error("Apple Token 검증 실패: Invalid issuer: {}", claims.getIssuer());
                return null;
            }
            // 토큰 만료 여부 확인
            Date now = new Date();
            if (claims.getExpirationTime() == null || claims.getExpirationTime().before(now)) {
                log.error("Apple Token 검증 실패: Token expired");
                return null;
            }
            // Audience 검증
            String clientId = "com.melissa.melissaFE"; // 실제 서비스의 client id
            if (!claims.getAudience().contains(clientId)) {
                log.error("Apple Token 검증 실패: Audience does not match. Expected: {}", clientId);
                return null;
            }
            // 6. 사용자 정보 추출
            String sub = claims.getSubject();
            String email = claims.getStringClaim("email");
            String name = generateRandomNickname();
            return ApplePayload.builder()
                    .sub(sub)
                    .email(email)
                    .name(name)
                    .build();
        } catch (Exception e) {
            log.error("Apple Token 검증 실패", e);
            return null;
        }
    }

    // 애플의 페이로드에서 name을 지원하지 않음.
    public String generateRandomNickname() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8); // 예: user-a1b2c3d4
    }


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
    @Builder
    @Getter
    public static class ApplePayload {
        private String sub;
        private String email;  // null 가능
        private String name;   // null 가능
    }
}