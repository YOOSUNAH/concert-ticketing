package com.concertticketing.domain.user.service;

import com.concertticketing.domain.user.entity.User;
import com.concertticketing.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserService userService;

    @Test
    @DisplayName("회원가입 성공 - 새 이메일이면 저장된다")
    void signUp_success() {
        // given - 이메일 중복 아님
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);

        // when - 회원가입 실행
        userService.signUp("test@test.com", "1234");

        // then - save()가 1번 호출됐는지 확인
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 중복 이메일이면 예외 발생")
    void signUp_duplicateEmail_throwsException() {
        // given - 이메일 이미 존재
        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        // when & then - 예외가 터져야 한다
        assertThrows(IllegalArgumentException.class,
                () -> userService.signUp("test@test.com", "1234"));

        // save()가 호출되지 않았는지 확인 (중복이면 저장하면 안 됨)
        verify(userRepository, never()).save(any());
    }
}
