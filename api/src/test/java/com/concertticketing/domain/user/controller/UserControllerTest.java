package com.concertticketing.domain.user.controller;

import com.concertticketing.domain.user.dto.SignUpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerTest {

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
    void 회원가입_성공() {
        SignUpRequest request = new SignUpRequest("test@example.com", "password123");

        webTestClient.post().uri("/users")
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();
    }
}
