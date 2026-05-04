package com.concertticketing.domain.auth.service;

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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    AuthService authService;

    @Test
    @DisplayName("로그인 성공 - 이메일/비밀번호 일치")
    void login_success() {
        // given
        User user = new User("test@test.com", "hashed-1234");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("1234", "hashed-1234")).thenReturn(true);

        // when
        String token = authService.login("test@test.com", "1234");

        // then
        assertNotNull(token);
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    void login_emailNotFound_throwsException() {
        // given
        when(userRepository.findByEmail("nope@test.com")).thenReturn(Optional.empty());

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("nope@test.com", "1234"));
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_wrongPassword_throwsException() {
        // given
        User user = new User("test@test.com", "hashed-1234");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed-1234")).thenReturn(false);

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("test@test.com", "wrong"));
    }
}
