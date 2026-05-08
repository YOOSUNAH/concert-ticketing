package com.concertticketing.domain.concert.controller;

import com.concertticketing.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

class ConcertControllerTest extends IntegrationTestBase {

    @Test
    void 콘서트_목록_조회() {
        webTestClient.get().uri("/concerts?page=0&size=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isArray()
                .jsonPath("$.content[0].title").isEqualTo("10cm 콘서트")
                .jsonPath("$.totalElements").isEqualTo(1);
    }

    @Test
    void 콘서트_상세_조회() {
        webTestClient.get().uri("/concerts/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.title").isEqualTo("10cm 콘서트")
                .jsonPath("$.venue").isEqualTo("올림픽공원 체조경기장")
                .jsonPath("$.schedules").isArray()
                .jsonPath("$.schedules.length()").isEqualTo(2);
    }
}
