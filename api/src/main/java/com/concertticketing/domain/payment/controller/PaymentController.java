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
        PaymentConfirmResponse response = new PaymentConfirmResponse(
                "BK20250801001", BookingStatus.PAID, List.of("A-1", "A-2"),
                "10cm 콘서트", "2025-08-01", "19:00", 237000, "2025-08-01T18:30:00"
        );
        return ResponseEntity.ok(response);
    }
}
