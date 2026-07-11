package com.concertticketing.queue.api.controller;

import com.concertticketing.domain.queue.service.QueueService;
import com.concertticketing.queue.api.dto.QueueEnterRequest;
import com.concertticketing.queue.api.dto.QueueEnterResponse;
import com.concertticketing.queue.api.support.QueueApiIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;

class QueueControllerTest extends QueueApiIntegrationTestBase {

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
        Long userId = 42L;
        String token = issueToken(userId);

        // 1. 대기열 입장 — queueToken 발급
        QueueEnterResponse enter = webTestClient.post().uri("/queue/enter")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(new QueueEnterRequest(1L))
                .exchange()
                .expectStatus().isOk()
                .expectBody(QueueEnterResponse.class)
                .returnResult().getResponseBody();

        assertThat(enter).isNotNull();
        assertThat(enter.getQueueToken()).isNotBlank();

        // 2. 스케줄러 수동 호출 (테스트 가속 — 실제는 queue-worker에서 3초 주기)
        queueService.processQueue(1L);

        // 3. status 조회 — ADMITTED + admissionToken 발급 확인
        webTestClient.get().uri("/queue/status?queueToken={t}", enter.getQueueToken())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ADMITTED")
                .jsonPath("$.admissionToken").exists();
    }

    @Test
    void 매진된_큐는_status_조회_시_SOLD_OUT_반환() {
        Long userId = 99L;
        String token = issueToken(userId);

        // 사용자가 큐에 진입
        QueueEnterResponse enter = webTestClient.post().uri("/queue/enter")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(new QueueEnterRequest(2L))
                .exchange()
                .expectStatus().isOk()
                .expectBody(QueueEnterResponse.class)
                .returnResult().getResponseBody();

        // 매진 → queue-worker가 큐 종료한 상태를 시뮬레이션
        queueService.closeSoldOutQueue(2L);

        // 폴링 응답에 SOLD_OUT 반환
        webTestClient.get().uri("/queue/status?queueToken={t}", enter.getQueueToken())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SOLD_OUT");
    }

    @Test
    void 매진된_큐에_신규_진입_시도_시_409() {
        Long userId = 7L;
        String token = issueToken(userId);

        // 큐 종료 상태 선설정
        queueService.closeSoldOutQueue(3L);

        webTestClient.post().uri("/queue/enter")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(new QueueEnterRequest(3L))
                .exchange()
                .expectStatus().isEqualTo(409);
    }
}
