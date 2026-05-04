package com.concertticketing.domain.auth.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32bytes-for-hs256";

    @Test
    @DisplayName("발급한 토큰을 파싱하면 userId가 그대로 복원된다")
    void createAndParse_roundTrip() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 3600);

        String token = provider.createToken(42L);
        Long userId = provider.getUserIdFromToken(token);

        assertEquals(42L, userId);
    }

    @Test
    @DisplayName("만료된 토큰은 IllegalArgumentException")
    void expiredToken_throwsException() throws InterruptedException {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 0); // 즉시 만료
        String token = provider.createToken(1L);
        Thread.sleep(50); // exp 시점이 지나도록 잠깐 대기

        assertThrows(IllegalArgumentException.class,
                () -> provider.getUserIdFromToken(token));
    }

    @Test
    @DisplayName("다른 시크릿으로 발급된 토큰은 검증 실패")
    void wrongSignature_throwsException() {
        JwtTokenProvider issuer = new JwtTokenProvider(SECRET, 3600);
        JwtTokenProvider verifier = new JwtTokenProvider(
                "different-secret-key-also-32bytes-or-longer", 3600);

        String token = issuer.createToken(1L);

        assertThrows(IllegalArgumentException.class,
                () -> verifier.getUserIdFromToken(token));
    }

    @Test
    @DisplayName("형식이 잘못된 토큰은 IllegalArgumentException")
    void malformedToken_throwsException() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 3600);

        assertThrows(IllegalArgumentException.class,
                () -> provider.getUserIdFromToken("not-a-jwt"));
    }
}
