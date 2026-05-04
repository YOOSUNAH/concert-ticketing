package com.concertticketing.domain.booking.controller;

import com.concertticketing.domain.booking.dto.BookingCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookingControllerTest {

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
    void 예매_생성_성공() {
        BookingCreateRequest request = new BookingCreateRequest(1L, List.of(101L, 102L), "admission-token");

        webTestClient.post().uri("/bookings")
                .header("Authorization", "Bearer test-token")
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.bookingIds").isArray()
                .jsonPath("$.bookingIds[0]").isEqualTo(999)
                .jsonPath("$.bookingIds[1]").isEqualTo(1000)
                .jsonPath("$.totalAmount").isEqualTo(242000);
    }

    @Test
    void 예매_내역_조회_성공() {
        webTestClient.get().uri("/bookings/me?page=0&size=10")
                .header("Authorization", "Bearer test-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isArray()
                .jsonPath("$.content[0].bookingId").isEqualTo(999)
                .jsonPath("$.content[0].concertTitle").isEqualTo("10cm 콘서트")
                .jsonPath("$.content[0].date").isEqualTo("2025-08-01")
                .jsonPath("$.content[0].time").isEqualTo("19:00")
                .jsonPath("$.content[0].seats").isArray()
                .jsonPath("$.content[0].seats[0]").isEqualTo("A-1")
                .jsonPath("$.content[0].seats[1]").isEqualTo("A-2")
                .jsonPath("$.content[0].totalAmount").isEqualTo(242000)
                .jsonPath("$.content[0].status").isEqualTo("PAID")
                .jsonPath("$.page").isEqualTo(0)
                .jsonPath("$.size").isEqualTo(10)
                .jsonPath("$.totalElements").isEqualTo(5)
                .jsonPath("$.totalPages").isEqualTo(1)
                .jsonPath("$.hasNext").isEqualTo(false);
    }

    @Test
    void 예매_내역_상세_조회_성공() {
        webTestClient.get().uri("/bookings/1")
                .header("Authorization", "Bearer test-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.bookingId").isEqualTo(1)
                .jsonPath("$.bookingNumber").isEqualTo("BK20250801001")
                .jsonPath("$.concertTitle").isEqualTo("10cm 콘서트")
                .jsonPath("$.venue").isEqualTo("올림픽공원")
                .jsonPath("$.date").isEqualTo("2025-08-01")
                .jsonPath("$.time").isEqualTo("19:00")
                .jsonPath("$.seats[0]").isEqualTo("A-1")
                .jsonPath("$.seats[1]").isEqualTo("A-2")
                .jsonPath("$.totalAmount").isEqualTo(242000)
                .jsonPath("$.paidAmount").isEqualTo(237000)
                .jsonPath("$.pointUsed").isEqualTo(5000)
                .jsonPath("$.paymentMethod").isEqualTo("CARD")
                .jsonPath("$.ticketType").isEqualTo("MOBILE")
                .jsonPath("$.status").isEqualTo("PAID")
                .jsonPath("$.paidAt").isEqualTo("2025-08-01T18:30:00");
    }

    @Test
    void 예매_취소_성공() {
        webTestClient.delete().uri("/bookings/1")
                .header("Authorization", "Bearer test-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.bookingId").isEqualTo(1)
                .jsonPath("$.bookingNumber").isEqualTo("BK20250801001")
                .jsonPath("$.status").isEqualTo("CANCELLED")
                .jsonPath("$.cancelledAmount").isEqualTo(242000)
                .jsonPath("$.cancelledAt").isEqualTo("2025-08-01T20:00:00");
    }
}
