package com.concertticketing.domain.auth.jwt;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32bytes-for-hs256";

    private static Cache<String, JwtTokenProvider.CachedToken> newCache() {
        return Caffeine.newBuilder().maximumSize(1_000).build();
    }

    @Test
    @DisplayName("발급한 토큰을 파싱하면 userId가 그대로 복원된다")
    void createAndParse_roundTrip() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 3600, newCache());

        String token = provider.createToken(42L);
        Long userId = provider.getUserIdFromToken(token);

        assertEquals(42L, userId);
    }

    @Test
    @DisplayName("만료된 토큰은 IllegalArgumentException")
    void expiredToken_throwsException() throws InterruptedException {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 0, newCache()); // 즉시 만료
        String token = provider.createToken(1L);
        Thread.sleep(50); // exp 시점이 지나도록 잠깐 대기

        assertThrows(IllegalArgumentException.class,
                () -> provider.getUserIdFromToken(token));
    }

    @Test
    @DisplayName("캐시에 남아있어도 토큰이 만료되면 캐시를 무시하고 예외를 던진다")
    void cachedButExpired_throwsException() throws InterruptedException {
        // 토큰 유효시간(1초)보다 캐시 보존시간(1시간)이 길어, 만료 후에도 캐시 엔트리는 살아있다.
        Cache<String, JwtTokenProvider.CachedToken> cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(1))
                .build();
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 1, cache);

        String token = provider.createToken(7L);
        assertEquals(7L, provider.getUserIdFromToken(token)); // 유효한 동안 캐시에 적재

        Thread.sleep(1_100); // 토큰만 만료 (캐시 엔트리는 그대로)

        assertThrows(IllegalArgumentException.class,
                () -> provider.getUserIdFromToken(token));
    }

    @Test
    @DisplayName("다른 시크릿으로 발급된 토큰은 검증 실패")
    void wrongSignature_throwsException() {
        JwtTokenProvider issuer = new JwtTokenProvider(SECRET, 3600, newCache());
        JwtTokenProvider verifier = new JwtTokenProvider(
                "different-secret-key-also-32bytes-or-longer", 3600, newCache());

        String token = issuer.createToken(1L);

        assertThrows(IllegalArgumentException.class,
                () -> verifier.getUserIdFromToken(token));
    }

    @Test
    @DisplayName("형식이 잘못된 토큰은 IllegalArgumentException")
    void malformedToken_throwsException() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 3600, newCache());

        assertThrows(IllegalArgumentException.class,
                () -> provider.getUserIdFromToken("not-a-jwt"));
    }
}
