package com.concertticketing.domain.auth.jwt;

import com.github.benmanes.caffeine.cache.Cache;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

public class JwtTokenProvider {

    private final SecretKey key;
    private final long validitySeconds;

    /** token → (userId, 토큰 만료시각) 캐시. 캐시 생성/설정은 외부에서 주입한다. */
    private final Cache<String, CachedToken> tokenCache;

    public JwtTokenProvider(String secret, long validitySeconds, Cache<String, CachedToken> tokenCache) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validitySeconds = validitySeconds;
        this.tokenCache = tokenCache;
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
     * 토큰에서 userId 추출
     * - 캐시에 있고 아직 만료 전이면 서명 검증을 건너뛰고 바로 반환
     * - 캐시에 없거나 캐시에 남아있더라도 이미 만료됐으면 다시 파싱·검증 (만료 토큰이면 예외)
     * - 서명 불일치, 만료, 형식 오류 시 IllegalArgumentException
     */
    public Long getUserIdFromToken(String token) {
        CachedToken cached = tokenCache.getIfPresent(token);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.userId();
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Long userId = Long.parseLong(claims.getSubject());
            Instant expiresAt = claims.getExpiration().toInstant();
            tokenCache.put(token, new CachedToken(userId, expiresAt));
            return userId;
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.", e);
        }
    }

    public record CachedToken(Long userId, Instant expiresAt) {
    }
}
