package com.concertticketing.domain.booking.controller;

import com.concertticketing.domain.auth.jwt.JwtTokenProvider;
import com.concertticketing.domain.booking.dto.BookingCreateRequest;
import com.concertticketing.domain.booking.dto.BookingCreateResponse;
import com.concertticketing.domain.queue.dto.QueueEntryResult;
import com.concertticketing.domain.queue.dto.QueueStatusResult;
import com.concertticketing.domain.queue.service.QueueService;
import com.concertticketing.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BookingControllerTest extends IntegrationTestBase {

    @Autowired
    QueueService queueService;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Test
    void 예매_생성_인증_없으면_401() {
        BookingCreateRequest request = new BookingCreateRequest(1L, List.of(1L), "any-token");

        webTestClient.post().uri("/bookings")
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void 예매_종단_시나리오_가입_로그인_대기열_예매_상세조회() {
        String token = signUpAndLogin("booker@test.com", "pw1234", "홍길동");
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        // 1. 대기열 입장 (queue-api 분리 후 QueueService 직접 호출)
        QueueEntryResult enter = queueService.enterQueue(userId, 1L);

        // 2. 스케줄러 트리거 → ADMITTED 전환
        queueService.processQueue(1L);

        // 3. status 조회 → admissionToken 획득
        QueueStatusResult status = queueService.getQueueStatus(userId, enter.getQueueToken());
        assertThat(status.getAdmissionToken()).isNotBlank();

        // 4. 예매 생성 (좌석 1번 = VIP, 121,000원)
        BookingCreateResponse created = webTestClient.post().uri("/bookings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(new BookingCreateRequest(1L, List.of(1L), status.getAdmissionToken()))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(BookingCreateResponse.class)
                .returnResult().getResponseBody();

        assertThat(created.getTotalAmount()).isEqualTo(121000);
        assertThat(created.getBookerName()).isEqualTo("홍길동");

        // 5. 예매 상세 조회
        webTestClient.get().uri("/bookings/{id}", created.getBookingId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.bookingId").isEqualTo(created.getBookingId())
                .jsonPath("$.concertTitle").isEqualTo("10cm 콘서트")
                .jsonPath("$.seats[0]").isEqualTo("A-1")
                .jsonPath("$.totalAmount").isEqualTo(121000)
                .jsonPath("$.status").isEqualTo("PENDING");
    }
}
