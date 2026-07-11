package com.concertticketing.domain.payment.controller;

import com.concertticketing.domain.auth.jwt.JwtTokenProvider;
import com.concertticketing.domain.booking.dto.BookingCreateRequest;
import com.concertticketing.domain.booking.dto.BookingCreateResponse;
import com.concertticketing.domain.payment.dto.PaymentConfirmRequest;
import com.concertticketing.domain.queue.dto.QueueEntryResult;
import com.concertticketing.domain.queue.dto.QueueStatusResult;
import com.concertticketing.domain.queue.service.QueueService;
import com.concertticketing.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import java.util.List;

class PaymentControllerTest extends IntegrationTestBase {

    @Autowired
    QueueService queueService;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Test
    void 결제_확정_성공() {
        String token = signUpAndLogin("pay@test.com", "pw1234", "홍길동");

        // 예매 생성까지 진행
        Long bookingId = createBooking(token);

        // 결제 확정 (VIP 121,000원, 포인트 0, paymentKey/orderId 임의)
        webTestClient.post().uri("/payments/confirm")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(new PaymentConfirmRequest(
                        bookingId, "test-pay-key", "test-order-id",
                        121000, 0, "CARD"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("PAID")
                .jsonPath("$.concertTitle").isEqualTo("10cm 콘서트")
                .jsonPath("$.paidAmount").isEqualTo(121000)
                .jsonPath("$.seats[0]").isEqualTo("A-1");
    }

    private Long createBooking(String token) {
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        // queue-api 분리 후 QueueService 직접 호출 (큐 엔드포인트는 queue-api에 있음)
        QueueEntryResult enter = queueService.enterQueue(userId, 1L);
        queueService.processQueue(1L);
        QueueStatusResult status = queueService.getQueueStatus(userId, enter.getQueueToken());

        BookingCreateResponse created = webTestClient.post().uri("/bookings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .bodyValue(new BookingCreateRequest(1L, List.of(1L), status.getAdmissionToken()))
                .exchange()
                .expectBody(BookingCreateResponse.class)
                .returnResult().getResponseBody();

        return created.getBookingId();
    }
}
