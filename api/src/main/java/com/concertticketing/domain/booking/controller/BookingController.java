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
        BookingCreateResponse response = new BookingCreateResponse(999L, 242000, "홍길동");
        return ResponseEntity.status(201).body(response);
    }

    // 예매 내역 조회 - Private
    @GetMapping("/me")
    public ResponseEntity<BookingListResponse> getMyBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        BookingListResponse.BookingItem item = new BookingListResponse.BookingItem(
                999L, "10cm 콘서트", "2025-08-01", "19:00",
                List.of("A-1", "A-2"), 242000, BookingStatus.PAID
        );
        BookingListResponse response = new BookingListResponse(List.of(item), page, size, 5L, 1, false);
        return ResponseEntity.ok(response);
    }

    // 예매 내역 상세 조회 - Private
    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingDetailResponse> getBooking(@PathVariable Long bookingId) {
        BookingDetailResponse response = new BookingDetailResponse(
                bookingId, "BK20250801001", "10cm 콘서트", "올림픽공원",
                "2025-08-01", "19:00", List.of("A-1", "A-2"),
                242000, 237000, 5000, "CARD", "MOBILE",
                BookingStatus.PAID, "2025-08-01T18:30:00"
        );
        return ResponseEntity.ok(response);
    }

    // 예매 취소 - Private
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<BookingCancelResponse> cancelBooking(@PathVariable Long bookingId) {
        BookingCancelResponse response = new BookingCancelResponse(
                bookingId, "BK20250801001", BookingStatus.CANCELLED,
                242000, "2025-08-01T20:00:00"
        );
        return ResponseEntity.ok(response);
    }
}
