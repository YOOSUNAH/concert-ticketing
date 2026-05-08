package com.concertticketing.domain.user.controller;

import com.concertticketing.domain.user.dto.SignUpRequest;
import com.concertticketing.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

class UserControllerTest extends IntegrationTestBase {

    @Test
    void 회원가입_성공() {
        SignUpRequest request = new SignUpRequest("test@example.com", "password123", "홍길동");

        webTestClient.post().uri("/users")
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void 회원가입_실패_중복_이메일() {
        SignUpRequest request = new SignUpRequest("dup@example.com", "password123", "홍길동");

        webTestClient.post().uri("/users")
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post().uri("/users")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
