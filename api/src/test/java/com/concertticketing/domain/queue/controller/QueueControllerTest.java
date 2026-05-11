package com.concertticketing.domain.queue.controller;

import com.concertticketing.domain.queue.dto.QueueEnterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

@Disabled
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class QueueControllerTest {

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
    void 대기열_입장_성공() {
        QueueEnterRequest request = new QueueEnterRequest(1L);

        webTestClient.post().uri("/queue/enter")
                .header("Authorization", "Bearer test-token")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.queueToken").isEqualTo("550e8400-e29b-41d4-a716-446655440000")
                .jsonPath("$.rank").isEqualTo(3842)
                .jsonPath("$.estimatedWaitSeconds").isEqualTo(192);
    }

    @Test
    void 대기열_순번_조회_성공() {
        webTestClient.get().uri("/queue/status?queueToken=550e8400-e29b-41d4-a716-446655440000")
                .header("Authorization", "Bearer test-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.rank").isEqualTo(120)
                .jsonPath("$.status").isEqualTo("WAITING");
    }
}
