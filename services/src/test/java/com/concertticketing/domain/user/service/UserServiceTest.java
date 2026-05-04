package com.concertticketing.domain.user.service;

import com.concertticketing.domain.user.entity.User;
import com.concertticketing.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    UserService userService;

    @Test
    @DisplayName("회원가입 성공 - 비밀번호는 해시되어 저장 + 포인트 0으로 시작")
    void signUp_success() {
        // given
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(passwordEncoder.encode("1234")).thenReturn("hashed-1234");

        // when
        userService.signUp("test@test.com", "1234");

        // then - 저장된 User의 password가 평문이 아닌 해시값
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("test@test.com", saved.getEmail());
        assertEquals("hashed-1234", saved.getPassword()); // 평문 "1234"가 아니라 인코딩된 값
        assertEquals(0, saved.getPoint());
    }

    @Test
    @DisplayName("회원가입 실패 - 중복 이메일이면 예외 발생, 인코딩도 호출 안 됨")
    void signUp_duplicateEmail_throwsException() {
        // given - 이메일 이미 존재
        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> userService.signUp("test@test.com", "1234"));

        verify(passwordEncoder, never()).encode(any()); // 중복 체크에서 차단되어 인코딩 도달 안 함
        verify(userRepository, never()).save(any());
    }
}
