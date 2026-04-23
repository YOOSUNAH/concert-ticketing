package com.concertticketing.domain.auth.controller;

import com.concertticketing.domain.auth.dto.LoginRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    // 로그인 - Public
    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody LoginRequest request, HttpServletResponse response) {
        response.setHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummy.token");
        return ResponseEntity.ok().build();
    }
}
