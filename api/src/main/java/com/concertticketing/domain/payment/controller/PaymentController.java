package com.concertticketing.domain.payment.controller;

import com.concertticketing.domain.booking.dto.BookingStatus;
import com.concertticketing.domain.payment.dto.PaymentConfirmRequest;
import com.concertticketing.domain.payment.dto.PaymentConfirmResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    // 결제 확정 - Private
    @PostMapping("/confirm")
    public ResponseEntity<PaymentConfirmResponse> confirmPayment(@RequestBody PaymentConfirmRequest request) {
        PaymentConfirmResponse response = PaymentConfirmResponse.builder()
                .bookingNumber("BK20250801001")
                .status(BookingStatus.PAID)
                .seats(List.of("A-1", "A-2"))
                .concertTitle("10cm 콘서트")
                .date("2025-08-01")
                .time("19:00")
                .paidAmount(237000)
                .paidAt("2025-08-01T18:30:00")
                .build();

        return ResponseEntity.ok(response);
    }
}
