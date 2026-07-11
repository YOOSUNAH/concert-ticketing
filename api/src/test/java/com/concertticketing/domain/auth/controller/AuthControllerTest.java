package com.concertticketing.domain.auth.controller;

import com.concertticketing.domain.auth.dto.LoginRequest;
import com.concertticketing.domain.user.dto.SignUpRequest;
import com.concertticketing.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerTest extends IntegrationTestBase {

    @Test
    void 로그인_성공_JWT_헤더_반환() {
        webTestClient.post().uri("/users")
                .bodyValue(new SignUpRequest("login@test.com", "pw1234", "홍길동"))
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post().uri("/auth/login")
                .bodyValue(new LoginRequest("login@test.com", "pw1234"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().value(HttpHeaders.AUTHORIZATION, value ->
                        assertThat(value).startsWith("Bearer "));
    }

    @Test
    void 로그인_실패_잘못된_비밀번호_400() {
        webTestClient.post().uri("/users")
                .bodyValue(new SignUpRequest("wrong@test.com", "pw1234", "홍길동"))
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post().uri("/auth/login")
                .bodyValue(new LoginRequest("wrong@test.com", "WRONG"))
                .exchange()
                .expectStatus().isBadRequest();
    }
}
