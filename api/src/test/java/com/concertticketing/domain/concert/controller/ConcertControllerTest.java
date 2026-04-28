package com.concertticketing.domain.concert.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConcertControllerTest {

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
    void 콘서트_목록_조회_성공() {
        webTestClient.get().uri("/concerts?page=0&size=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isArray()
                .jsonPath("$.content[0].concertId").isEqualTo(1)
                .jsonPath("$.content[0].title").isEqualTo("10cm 콘서트")
                .jsonPath("$.content[0].artist").isEqualTo("10cm")
                .jsonPath("$.content[0].thumbnailUrl").isNotEmpty()
                .jsonPath("$.content[0].venue").isEqualTo("올림픽공원")
                .jsonPath("$.content[0].startDate").isEqualTo("2025-08-01")
                .jsonPath("$.content[0].endDate").isEqualTo("2025-08-02")
                .jsonPath("$.content[0].status").isEqualTo("OPEN")
                .jsonPath("$.page").isEqualTo(0)
                .jsonPath("$.size").isEqualTo(10)
                .jsonPath("$.totalElements").isEqualTo(50)
                .jsonPath("$.totalPages").isEqualTo(5)
                .jsonPath("$.hasNext").isEqualTo(true);
    }

    @Test
    void 콘서트_상세_조회_성공() {
        webTestClient.get().uri("/concerts/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.concertId").isEqualTo(1)
                .jsonPath("$.title").isEqualTo("10cm 콘서트")
                .jsonPath("$.artist").isEqualTo("10cm")
                .jsonPath("$.description").isEqualTo("공연 설명")
                .jsonPath("$.venue").isEqualTo("올림픽공원")
                .jsonPath("$.posterUrl").isNotEmpty()
                .jsonPath("$.maxTicketsPerPerson").isEqualTo(2)
                .jsonPath("$.status").isEqualTo("OPEN")
                .jsonPath("$.schedules").isArray()
                .jsonPath("$.schedules[0].scheduleId").isEqualTo(1)
                .jsonPath("$.schedules[0].date").isEqualTo("2025-08-01")
                .jsonPath("$.schedules[0].time").isEqualTo("19:00")
                .jsonPath("$.schedules[0].totalSeats").isEqualTo(500)
                .jsonPath("$.schedules[0].remainingSeats").isEqualTo(120);
    }
}
