package com.concertticketing.domain.user.controller;

import com.concertticketing.domain.user.dto.SignUpRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    // 회원가입 - Public
    @PostMapping
    public ResponseEntity<Void> signUp(@RequestBody SignUpRequest request) {
        return ResponseEntity.status(201).build();
    }
}
