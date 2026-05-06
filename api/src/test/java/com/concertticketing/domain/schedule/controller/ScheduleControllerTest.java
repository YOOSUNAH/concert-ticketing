package com.concertticketing.domain.schedule.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

@Disabled
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ScheduleControllerTest {

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
    void 좌석_목록_조회_성공() {
        webTestClient.get().uri("/schedules/1/seats")
                .header("Authorization", "Bearer test-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.seats").isArray()
                .jsonPath("$.seats[0].seatId").isEqualTo(101)
                .jsonPath("$.seats[0].seatNumber").isEqualTo("A-1")
                .jsonPath("$.seats[0].grade").isEqualTo("VIP")
                .jsonPath("$.seats[0].price").isEqualTo(121000)
                .jsonPath("$.seats[0].status").isEqualTo("AVAILABLE");
    }
}
