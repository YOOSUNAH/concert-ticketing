package com.concertticketing.domain.payment.controller;

import com.concertticketing.domain.payment.dto.PaymentConfirmRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentControllerTest {

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
    void 결제_확정_성공() {
        PaymentConfirmRequest request = new PaymentConfirmRequest(
                1L, "paykey_123", "order_abc", 237000, 5000, "CARD"
        );

        webTestClient.post().uri("/payments/confirm")
                .header("Authorization", "Bearer test-token")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.bookingNumber").isEqualTo("BK20250801001")
                .jsonPath("$.status").isEqualTo("PAID")
                .jsonPath("$.seats").isArray()
                .jsonPath("$.seats[0]").isEqualTo("A-1")
                .jsonPath("$.seats[1]").isEqualTo("A-2")
                .jsonPath("$.concertTitle").isEqualTo("10cm 콘서트")
                .jsonPath("$.date").isEqualTo("2025-08-01")
                .jsonPath("$.time").isEqualTo("19:00")
                .jsonPath("$.paidAmount").isEqualTo(237000)
                .jsonPath("$.paidAt").isEqualTo("2025-08-01T18:30:00");
    }
}
