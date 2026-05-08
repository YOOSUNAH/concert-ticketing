package com.concertticketing.domain.queue.controller;

import com.concertticketing.domain.queue.dto.QueueEnterRequest;
import com.concertticketing.domain.queue.dto.QueueEnterResponse;
import com.concertticketing.domain.queue.service.QueueService;
import com.concertticketing.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;

class QueueControllerTest extends IntegrationTestBase {

    @Autowired
    QueueService queueService;

    @Test
    void 대기열_입장_인증_없으면_401() {
        webTestClient.post().uri("/queue/enter")
                .bodyValue(new QueueEnterRequest(1L))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void 대기열_enter_processQueue_status_흐름_ADMITTED_확인() {
        String token = signUpAndLogin("queue@test.com", "pw1234", "홍길동");

        // 1. 대기열 입장 — queueToken 발급
        QueueEnterResponse enterResponse = webTestClient.post().uri("/queue/enter")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(new QueueEnterRequest(1L))
                .exchange()
                .expectStatus().isOk()
                .expectBody(QueueEnterResponse.class)
                .returnResult().getResponseBody();

        assertThat(enterResponse).isNotNull();
        assertThat(enterResponse.getQueueToken()).isNotBlank();

        // 2. 스케줄러 수동 호출 (테스트 가속 — 실제는 3초 주기)
        queueService.processQueue(1L);

        // 3. status 조회 — ADMITTED + admissionToken 발급 확인
        webTestClient.get().uri("/queue/status?queueToken={t}", enterResponse.getQueueToken())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ADMITTED")
                .jsonPath("$.admissionToken").exists();
    }
}
