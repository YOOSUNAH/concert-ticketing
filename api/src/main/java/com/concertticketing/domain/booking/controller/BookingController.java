package com.concertticketing.domain.booking.controller;

import com.concertticketing.auth.AuthUserId;
import com.concertticketing.domain.booking.dto.BookingCancelResponse;
import com.concertticketing.domain.booking.dto.BookingCreateRequest;
import com.concertticketing.domain.booking.dto.BookingCreateResponse;
import com.concertticketing.domain.booking.dto.BookingDetailResponse;
import com.concertticketing.domain.booking.dto.BookingListResponse;
import com.concertticketing.domain.booking.dto.BookingStatus;
import com.concertticketing.domain.booking.entity.Booking;
import com.concertticketing.domain.booking.service.BookingService;
import com.concertticketing.domain.concert.entity.Concert;
import com.concertticketing.domain.concert.service.ConcertService;
import com.concertticketing.domain.payment.entity.Payment;
import com.concertticketing.domain.payment.service.PaymentService;
import com.concertticketing.domain.schedule.entity.Schedule;
import com.concertticketing.domain.seat.entity.Seat;
import com.concertticketing.domain.seat.service.SeatService;
import com.concertticketing.domain.user.entity.User;
import com.concertticketing.domain.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final UserService userService;
    private final ConcertService concertService;
    private final SeatService seatService;
    private final PaymentService paymentService;

    public BookingController(BookingService bookingService,
                             UserService userService,
                             ConcertService concertService,
                             SeatService seatService,
                             PaymentService paymentService) {
        this.bookingService = bookingService;
        this.userService = userService;
        this.concertService = concertService;
        this.seatService = seatService;
        this.paymentService = paymentService;
    }

    // 예매 생성 - Private
    @PostMapping
    public ResponseEntity<BookingCreateResponse> createBooking(
            @AuthUserId Long userId,
            @RequestBody BookingCreateRequest request
    ) {
        Booking booking = bookingService.createBooking(
                userId, request.getScheduleId(), request.getSeatIds(), request.getAdmissionToken()
        );
        User user = userService.getUser(userId);

        return ResponseEntity.status(201).body(new BookingCreateResponse(
                booking.getId(),
                booking.getTotalAmount(),
                user.getName()
        ));
    }

    // 예매 내역 조회 - Private
    @GetMapping("/me")
    public ResponseEntity<BookingListResponse> getMyBookings(
            @AuthUserId Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<Booking> bookings = bookingService.getMyBookings(userId, page, size);
        long total = bookingService.getMyBookingsCount(userId);
        int totalPages = (int) Math.ceil((double) total / size);
        boolean hasNext = page + 1 < totalPages;

        // 일괄 조회: schedule, concert, seat을 ID 모아서 한 번에
        List<Long> scheduleIds = bookings.stream()
                .map(Booking::getScheduleId).distinct().toList();
        List<Schedule> schedules = concertService.getSchedulesByIds(scheduleIds);
        Map<Long, Schedule> scheduleById = schedules.stream()
                .collect(Collectors.toMap(Schedule::getId, s -> s));

        List<Long> concertIds = schedules.stream()
                .map(Schedule::getConcertId).distinct().toList();
        Map<Long, Concert> concertById = concertService.getConcertsByIds(concertIds).stream()
                .collect(Collectors.toMap(Concert::getId, c -> c));

        List<Long> allSeatIds = bookings.stream()
                .flatMap(b -> b.getSeatIds().stream()).distinct().toList();
        Map<Long, Seat> seatById = seatService.getSeatsByIds(allSeatIds).stream()
                .collect(Collectors.toMap(Seat::getId, s -> s));

        List<BookingListResponse.BookingItem> items = bookings.stream()
                .map(b -> mapToItem(b, scheduleById, concertById, seatById))
                .toList();

        return ResponseEntity.ok(
                new BookingListResponse(items, page, size, total, totalPages, hasNext)
        );
    }

    // 예매 내역 상세 조회 - Private
    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingDetailResponse> getBooking(
            @AuthUserId Long userId,
            @PathVariable Long bookingId
    ) {
        Booking booking = bookingService.getBookingDetail(bookingId, userId);
        Schedule schedule = concertService.getSchedule(booking.getScheduleId());
        Concert concert = concertService.getConcertByScheduleId(booking.getScheduleId());
        List<String> seatNumbers = seatService.getSeatsByIds(booking.getSeatIds()).stream()
                .map(Seat::getSeatNumber)
                .toList();
        Optional<Payment> paymentOpt = paymentService.getPaymentByBookingId(bookingId);

        int paidAmount = paymentOpt.map(Payment::getAmount).orElse(0);
        int pointUsed = paymentOpt.map(Payment::getPointUsed).orElse(0);
        String paymentMethod = paymentOpt.map(Payment::getPaymentMethod).orElse(null);
        String paidAt = paymentOpt.map(p -> p.getPaidAt().toString()).orElse(null);

        return ResponseEntity.ok(new BookingDetailResponse(
                booking.getId(),
                booking.getBookingNumber(),
                concert.getTitle(),
                concert.getVenue(),
                schedule.getDate().toString(),
                schedule.getTime().toString(),
                seatNumbers,
                booking.getTotalAmount(),
                paidAmount,
                pointUsed,
                paymentMethod,
                "MOBILE",
                BookingStatus.valueOf(booking.getStatus().name()),
                paidAt
        ));
    }

    // 예매 취소 - Private
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<BookingCancelResponse> cancelBooking(
            @AuthUserId Long userId,
            @PathVariable Long bookingId
    ) {
        int refundedAmount = paymentService.getPaymentByBookingId(bookingId)
                .map(p -> p.getAmount() + p.getPointUsed())
                .orElse(0);

        Booking cancelled = bookingService.cancelBooking(bookingId, userId);

        return ResponseEntity.ok(new BookingCancelResponse(
                cancelled.getId(),
                cancelled.getBookingNumber(),
                BookingStatus.valueOf(cancelled.getStatus().name()),
                refundedAmount,
                cancelled.getCancelledAt() != null ? cancelled.getCancelledAt().toString() : LocalDateTime.now().toString()
        ));
    }

    private BookingListResponse.BookingItem mapToItem(Booking booking,
                                                      Map<Long, Schedule> scheduleById,
                                                      Map<Long, Concert> concertById,
                                                      Map<Long, Seat> seatById) {
        Schedule schedule = scheduleById.get(booking.getScheduleId());
        Concert concert = concertById.get(schedule.getConcertId());
        List<String> seatNumbers = booking.getSeatIds().stream()
                .map(seatById::get)
                .map(Seat::getSeatNumber)
                .toList();

        return new BookingListResponse.BookingItem(
                booking.getId(),
                concert.getTitle(),
                schedule.getDate().toString(),
                schedule.getTime().toString(),
                seatNumbers,
                booking.getTotalAmount(),
                BookingStatus.valueOf(booking.getStatus().name())
        );
    }
}
