package com.concertticketing.domain.payment.controller;

import com.concertticketing.auth.AuthUserId;
import com.concertticketing.domain.booking.dto.BookingStatus;
import com.concertticketing.domain.booking.entity.Booking;
import com.concertticketing.domain.booking.service.BookingFacade;
import com.concertticketing.domain.booking.service.BookingService;
import com.concertticketing.domain.concert.entity.ConcertRef;
import com.concertticketing.domain.concert.service.ConcertRefService;
import com.concertticketing.domain.payment.dto.PaymentConfirmRequest;
import com.concertticketing.domain.payment.dto.PaymentConfirmResponse;
import com.concertticketing.domain.payment.entity.Payment;
import com.concertticketing.domain.payment.gateway.PaymentGatewayException;
import com.concertticketing.domain.payment.service.PaymentService;
import com.concertticketing.domain.schedule.entity.ScheduleRef;
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
    private final BookingFacade bookingFacade;
    private final BookingService bookingService;
    private final ConcertRefService concertRefService;
    private final SeatService seatService;

    public PaymentController(PaymentService paymentService,
                             BookingFacade bookingFacade,
                             BookingService bookingService,
                             ConcertRefService concertRefService,
                             SeatService seatService) {
        this.paymentService = paymentService;
        this.bookingFacade = bookingFacade;
        this.bookingService = bookingService;
        this.concertRefService = concertRefService;
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
            bookingFacade.failBooking(request.getBookingId());
            return ResponseEntity.status(400).body(Map.of("code", "PAYMENT_FAILED"));
        }
    }

    private PaymentConfirmResponse toSuccessResponse(Payment payment, Long userId) {
        Booking booking = bookingService.getBookingDetail(payment.getBookingId(), userId);
        ScheduleRef schedule = concertRefService.getSchedule(booking.getScheduleId());
        ConcertRef concert = concertRefService.getConcertByScheduleId(booking.getScheduleId());
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
