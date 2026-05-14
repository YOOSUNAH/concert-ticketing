package com.concertticketing.concert.api.controller;

import com.concertticketing.concert.api.support.ConcertApiIntegrationTestBase;
import org.junit.jupiter.api.Test;

class ConcertControllerTest extends ConcertApiIntegrationTestBase {

    @Test
    void 콘서트_목록_조회() {
        seedConcert(1L, "10cm 콘서트");
        seedConcert(2L, "임영웅 콘서트");

        webTestClient.get().uri("/concerts?page=0&size=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(2)
                .jsonPath("$.totalElements").isEqualTo(2);
    }

    @Test
    void 콘서트_상세_조회_임베디드_schedules_포함() {
        seedConcert(1L, "10cm 콘서트");

        webTestClient.get().uri("/concerts/{id}", 1L)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.concertId").isEqualTo(1)
                .jsonPath("$.title").isEqualTo("10cm 콘서트")
                .jsonPath("$.schedules.length()").isEqualTo(1)
                .jsonPath("$.schedules[0].scheduleId").isEqualTo(11);
    }

    @Test
    void 콘서트_상세_조회_없으면_400() {
        webTestClient.get().uri("/concerts/{id}", 999L)
                .exchange()
                .expectStatus().is5xxServerError();
    }
}
