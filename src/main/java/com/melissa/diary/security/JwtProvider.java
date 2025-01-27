package com.melissa.diary.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtProvider {

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    // 유효 기간
    private final long ACCESS_TOKEN_VALID_MILLIS = 1000L * 60 * 60 * 100;       // 1시간
    private final long REFRESH_TOKEN_VALID_MILLIS = 1000L * 60 * 60 * 24 * 100; // 1일

    // Access Token 생성
    public String createAccessToken(Long userId, String provider) {
        return createToken(userId, provider, ACCESS_TOKEN_VALID_MILLIS);
    }

    // Refresh Token 생성
    public String createRefreshToken(Long userId, String provider) {
        return createToken(userId, provider, REFRESH_TOKEN_VALID_MILLIS);
    }

    private String createToken(Long userId, String provider, long validMillis) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validMillis);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("provider", provider)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    // JWT 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(secretKey)
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            // 만료 or 위조
            return false;
        }
    }

    // JWT에서 userId 추출
    public Long getUserId(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody();
        return Long.valueOf(claims.getSubject());
    }

}