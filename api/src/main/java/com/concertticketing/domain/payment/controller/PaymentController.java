package com.concertticketing.domain.payment.controller;

import com.concertticketing.auth.AuthUserId;
import com.concertticketing.domain.booking.dto.BookingStatus;
import com.concertticketing.domain.booking.entity.Booking;
import com.concertticketing.domain.booking.service.BookingService;
import com.concertticketing.domain.concert.entity.Concert;
import com.concertticketing.domain.concert.service.ConcertService;
import com.concertticketing.domain.payment.dto.PaymentConfirmRequest;
import com.concertticketing.domain.payment.dto.PaymentConfirmResponse;
import com.concertticketing.domain.payment.entity.Payment;
import com.concertticketing.domain.payment.gateway.PaymentGatewayException;
import com.concertticketing.domain.payment.service.PaymentService;
import com.concertticketing.domain.schedule.entity.Schedule;
import com.concertticketing.domain.seat.entity.Seat;
import com.concertticketing.domain.seat.service.SeatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final BookingService bookingService;
    private final ConcertService concertService;
    private final SeatService seatService;

    public PaymentController(PaymentService paymentService,
                             BookingService bookingService,
                             ConcertService concertService,
                             SeatService seatService) {
        this.paymentService = paymentService;
        this.bookingService = bookingService;
        this.concertService = concertService;
        this.seatService = seatService;
    }

    // 결제 확정 - Private
    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(
            @AuthUserId Long userId,
            @RequestBody PaymentConfirmRequest request
    ) {
        try {
            Payment payment = paymentService.confirmPayment(
                    request.getBookingId(),
                    request.getPaymentKey(),
                    request.getOrderId(),
                    request.getAmount(),
                    request.getPointUsed(),
                    request.getPaymentMethod()
            );

            return ResponseEntity.ok(toSuccessResponse(payment, userId));
        } catch (PaymentGatewayException e) {
            // PG 실패 → 예매 실패 처리 (좌석 복구 + booking CANCELLED)
            bookingService.failBooking(request.getBookingId());
            return ResponseEntity.status(400).body(Map.of("code", "PAYMENT_FAILED"));
        }
    }

    private PaymentConfirmResponse toSuccessResponse(Payment payment, Long userId) {
        Booking booking = bookingService.getBookingDetail(payment.getBookingId(), userId);
        Schedule schedule = concertService.getSchedule(booking.getScheduleId());
        Concert concert = concertService.getConcertByScheduleId(booking.getScheduleId());
        List<String> seatNumbers = seatService.getSeatsByIds(booking.getSeatIds()).stream()
                .map(Seat::getSeatNumber)
                .toList();

        return new PaymentConfirmResponse(
                booking.getBookingNumber(),
                BookingStatus.valueOf(booking.getStatus().name()),
                seatNumbers,
                concert.getTitle(),
                schedule.getDate().toString(),
                schedule.getTime().toString(),
                payment.getAmount(),
                payment.getPaidAt().toString()
        );
    }
}
