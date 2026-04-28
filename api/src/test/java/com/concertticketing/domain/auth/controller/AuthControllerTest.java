package com.concertticketing.domain.auth.controller;

import com.concertticketing.domain.auth.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void 로그인_성공() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        webTestClient.post().uri("/auth/login")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueMatches("Authorization", "Bearer .*");
    }
}
