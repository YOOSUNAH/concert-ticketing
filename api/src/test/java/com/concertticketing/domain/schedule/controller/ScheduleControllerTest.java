package com.concertticketing.domain.schedule.controller;

import com.concertticketing.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class ScheduleControllerTest extends IntegrationTestBase {

    @Test
    void 좌석_목록_조회_인증_없으면_401() {
        webTestClient.get().uri("/schedules/1/seats")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void 좌석_목록_조회_성공() {
        String token = signUpAndLogin("schedule@test.com", "pw1234", "홍길동");

        webTestClient.get().uri("/schedules/1/seats")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.seats").isArray()
                .jsonPath("$.seats.length()").isEqualTo(6)
                .jsonPath("$.seats[0].seatNumber").exists()
                .jsonPath("$.seats[0].status").isEqualTo("AVAILABLE");
    }
}
