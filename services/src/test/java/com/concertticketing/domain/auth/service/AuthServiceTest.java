package com.concertticketing.domain.auth.service;

import com.concertticketing.domain.auth.jwt.JwtTokenProvider;
import com.concertticketing.domain.user.entity.User;
import com.concertticketing.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    AuthService authService;

    @Test
    @DisplayName("로그인 성공 - JWT 발급되어 반환됨")
    void login_success() {
        // given
        User user = new User("test@test.com", "hashed-1234", "홍길동");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("1234", "hashed-1234")).thenReturn(true);
        when(jwtTokenProvider.createToken(any())).thenReturn("jwt-token-string");

        // when
        String token = authService.login("test@test.com", "1234");

        // then
        assertEquals("jwt-token-string", token);
        verify(jwtTokenProvider).createToken(user.getId()); // userId로 토큰 발급
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    void login_emailNotFound_throwsException() {
        // given
        when(userRepository.findByEmail("nope@test.com")).thenReturn(Optional.empty());

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("nope@test.com", "1234"));

        verify(jwtTokenProvider, never()).createToken(any()); // 토큰 발급도 안 됨
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_wrongPassword_throwsException() {
        // given
        User user = new User("test@test.com", "hashed-1234", "홍길동");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed-1234")).thenReturn(false);

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("test@test.com", "wrong"));

        verify(jwtTokenProvider, never()).createToken(any()); // 토큰 발급도 안 됨
    }
}
