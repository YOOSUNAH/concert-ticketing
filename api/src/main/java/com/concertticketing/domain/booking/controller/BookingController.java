package com.concertticketing.domain.booking.controller;

import com.concertticketing.domain.booking.dto.BookingCancelResponse;
import com.concertticketing.domain.booking.dto.BookingCreateRequest;
import com.concertticketing.domain.booking.dto.BookingCreateResponse;
import com.concertticketing.domain.booking.dto.BookingDetailResponse;
import com.concertticketing.domain.booking.dto.BookingListResponse;
import com.concertticketing.domain.booking.dto.BookingStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    // 예매 생성 - Private
    @PostMapping
    public ResponseEntity<BookingCreateResponse> createBooking(@RequestBody BookingCreateRequest request) {
        BookingCreateResponse response = BookingCreateResponse.builder()
                .bookingId(999L)
                .totalAmount(242000)
                .bookerName("홍길동")
                .build();

        return ResponseEntity.status(201).body(response);
    }

    // 예매 내역 조회 - Private
    @GetMapping("/me")
    public ResponseEntity<BookingListResponse> getMyBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        BookingListResponse.BookingItem item = BookingListResponse.BookingItem.builder()
                .bookingId(999L)
                .concertTitle("10cm 콘서트")
                .date("2025-08-01")
                .time("19:00")
                .seats(List.of("A-1", "A-2"))
                .totalAmount(242000)
                .status(BookingStatus.PAID)
                .build();

        BookingListResponse response = BookingListResponse.builder()
                .content(List.of(item))
                .page(page)
                .size(size)
                .totalElements(5)
                .totalPages(1)
                .hasNext(false)
                .build();

        return ResponseEntity.ok(response);
    }

    // 예매 내역 상세 조회 - Private
    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingDetailResponse> getBooking(@PathVariable Long bookingId) {
        BookingDetailResponse response = BookingDetailResponse.builder()
                .bookingId(bookingId)
                .bookingNumber("BK20250801001")
                .concertTitle("10cm 콘서트")
                .venue("올림픽공원")
                .date("2025-08-01")
                .time("19:00")
                .seats(List.of("A-1", "A-2"))
                .totalAmount(242000)
                .paidAmount(237000)
                .pointUsed(5000)
                .paymentMethod("CARD")
                .ticketType("MOBILE")
                .status(BookingStatus.PAID)
                .paidAt("2025-08-01T18:30:00")
                .build();

        return ResponseEntity.ok(response);
    }

    // 예매 취소 - Private
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<BookingCancelResponse> cancelBooking(@PathVariable Long bookingId) {
        BookingCancelResponse response = BookingCancelResponse.builder()
                .bookingId(bookingId)
                .bookingNumber("BK20250801001")
                .status(BookingStatus.CANCELLED)
                .cancelledAmount(242000)
                .cancelledAt("2025-08-01T20:00:00")
                .build();

        return ResponseEntity.ok(response);
    }
}
