package com.melissa.diary.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtProvider {

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    private static final String CLAIM_TOKEN_TYPE = "typ";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private Key signingKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    // 유효 기간
    private final long ACCESS_TOKEN_VALID_MILLIS = 1000L * 60 * 60 * 24;       // 1일
    private final long REFRESH_TOKEN_VALID_MILLIS = 1000L * 60 * 60 * 24 * 15; // 15일

    // Access Token 생성
    public String createAccessToken(Long userId, String provider) {
        return createToken(userId, provider, ACCESS_TOKEN_VALID_MILLIS, JwtTokenType.ACCESS);
    }

    // Refresh Token 생성
    public String createRefreshToken(Long userId, String provider) {
        return createToken(userId, provider, REFRESH_TOKEN_VALID_MILLIS, JwtTokenType.REFRESH);
    }

    private String createToken(Long userId, String provider, long validMillis, JwtTokenType tokenType) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validMillis);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("provider", provider)
                .claim(CLAIM_TOKEN_TYPE, tokenType == JwtTokenType.ACCESS ? TOKEN_TYPE_ACCESS : TOKEN_TYPE_REFRESH)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // JWT 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(signingKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            // 만료 or 위조
            return false;
        }
    }

    public TokenValidationResult validateTokenResult(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(signingKey())
                    .build()
                    .parseClaimsJws(token);
            return TokenValidationResult.VALID;
        } catch (ExpiredJwtException e) {
            return TokenValidationResult.EXPIRED;
        } catch (JwtException | IllegalArgumentException e) {
            return TokenValidationResult.INVALID;
        }
    }

    // JWT에서 userId 추출
    public Long getUserId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return Long.valueOf(claims.getSubject());
    }

    public JwtTokenType getTokenTypeOrNull(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        Object typ = claims.get(CLAIM_TOKEN_TYPE);
        if (!(typ instanceof String)) return null;
        String v = (String) typ;
        if (TOKEN_TYPE_ACCESS.equalsIgnoreCase(v)) return JwtTokenType.ACCESS;
        if (TOKEN_TYPE_REFRESH.equalsIgnoreCase(v)) return JwtTokenType.REFRESH;
        return null;
    }

    public boolean isAccessToken(String token) {
        return getTokenTypeOrNull(token) == JwtTokenType.ACCESS;
    }

    public boolean isRefreshToken(String token) {
        return getTokenTypeOrNull(token) == JwtTokenType.REFRESH;
    }

}