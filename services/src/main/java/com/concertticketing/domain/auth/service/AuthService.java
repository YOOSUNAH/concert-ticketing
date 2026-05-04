package com.concertticketing.domain.auth.service;

import com.concertticketing.domain.user.entity.User;
import com.concertticketing.domain.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 로그인 → 토큰 반환
     */
    public String login(String email, String password) {
        // 1. 이메일로 유저 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));

        // 2. 비밀번호 확인 (해시 비교)
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 3. 토큰 생성 (지금은 단순 문자열, 나중에 JWT로 교체)
        return "token-" + user.getId();
    }
}
