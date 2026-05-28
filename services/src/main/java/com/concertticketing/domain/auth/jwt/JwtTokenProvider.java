package com.concertticketing.domain.auth.jwt;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

public class JwtTokenProvider {

    private final SecretKey key;
    private final long validitySeconds;

    /** token → userId 캐시. 토큰 유효시간 기반으로 자동 만료. */
    private final Cache<String, Long> tokenCache;

    public JwtTokenProvider(String secret, long validitySeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validitySeconds = validitySeconds;
        this.tokenCache = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofSeconds(validitySeconds))
                .build();
    }

    /**
     * 토큰 발급 (sub=userId, iat, exp)
     */
    public String createToken(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(validitySeconds)))
                .signWith(key)
                .compact();
    }

    /**
     * 토큰에서 userId 추출 (캐시 우선, 없으면 파싱 후 캐시에 저장)
     * - 서명 불일치, 만료, 형식 오류 시 IllegalArgumentException
     */
    public Long getUserIdFromToken(String token) {
        Long cached = tokenCache.getIfPresent(token);
        if (cached != null) {
            return cached;
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId = Long.parseLong(claims.getSubject());
            tokenCache.put(token, userId);
            return userId;
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.", e);
        }
    }
}
