package com.concertticketing.domain.user.service;

import com.concertticketing.domain.user.entity.User;
import com.concertticketing.domain.user.repository.UserRepository;

public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 회원가입
     */
    public void signUp(String email, String password) {
        // 1. 중복 이메일 체크
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        // 2. 유저 생성 & 저장
        User user = new User(email, password);
        userRepository.save(user);
    }
}
