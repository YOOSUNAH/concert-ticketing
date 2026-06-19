package com.concertticketing.domain.booking.controller;

import com.concertticketing.auth.AuthUserId;
import com.concertticketing.domain.booking.dto.BookingCancelResponse;
import com.concertticketing.domain.booking.dto.BookingCreateRequest;
import com.concertticketing.domain.booking.dto.BookingCreateResponse;
import com.concertticketing.domain.booking.dto.BookingDetailResponse;
import com.concertticketing.domain.booking.dto.BookingListResponse;
import com.concertticketing.domain.booking.dto.BookingStatus;
import com.concertticketing.domain.booking.entity.Booking;
import com.concertticketing.domain.booking.service.BookingFacade;
import com.concertticketing.domain.booking.service.BookingService;
import com.concertticketing.domain.payment.entity.Payment;
import com.concertticketing.domain.payment.service.PaymentService;
import com.concertticketing.domain.seat.entity.Seat;
import com.concertticketing.domain.seat.service.SeatService;
import com.concertticketing.domain.user.entity.User;
import com.concertticketing.domain.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private static final Logger log = LoggerFactory.getLogger(BookingController.class);

    private final BookingFacade bookingFacade;
    private final BookingService bookingService;
    private final UserService userService;
    private final SeatService seatService;
    private final PaymentService paymentService;
    private final Executor ioExecutor;

    public BookingController(BookingFacade bookingFacade,
                             BookingService bookingService,
                             UserService userService,
                             SeatService seatService,
                             PaymentService paymentService,
                             Executor ioExecutor) {
        this.bookingFacade = bookingFacade;
        this.bookingService = bookingService;
        this.userService = userService;
        this.seatService = seatService;
        this.paymentService = paymentService;
        this.ioExecutor = ioExecutor;
    }

    // 예매 생성 - Private
    @PostMapping
    public ResponseEntity<BookingCreateResponse> createBooking(
            @AuthUserId Long userId,
            @RequestBody BookingCreateRequest request
    ) {
        Booking booking = bookingFacade.createBooking(
                userId, request.getScheduleId(), request.getSeatIds(), request.getAdmissionToken()
        );
        User user = userService.getUser(userId);

        return ResponseEntity.status(201).body(new BookingCreateResponse(
                booking.getId(),
                booking.getTotalAmount(),
                user.getName()
        ));
    }

    // 예매 내역 조회 - Private (CompletableFuture 비동기 전환)
    @GetMapping("/me")
    public CompletableFuture<ResponseEntity<BookingListResponse>> getMyBookings(
            @AuthUserId Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // [1] 톰캣 워커 스레드(http-nio-*). CompletableFuture를 반환하는 순간 이 스레드는 즉시 반납된다.
        log.info("[1] controller 진입 thread = {}", Thread.currentThread().getName());

        // (A) 서로 독립적인 두 조회를 ioExecutor에서 병렬 실행
        CompletableFuture<List<Booking>> bookingsFuture = CompletableFuture.supplyAsync(() -> {
            log.info("[2] getMyBookings(목록) 처리 thread = {}", Thread.currentThread().getName());
            return bookingService.getMyBookings(userId, page, size);
        }, ioExecutor);

        CompletableFuture<Long> countFuture = CompletableFuture.supplyAsync(() -> {
            log.info("[3] getMyBookingsCount(개수) 처리 thread = {}", Thread.currentThread().getName());
            return bookingService.getMyBookingsCount(userId);
        }, ioExecutor);

        // (B) 두 독립 결과를 합성 (thenCombine)
        return bookingsFuture
                .thenCombine(countFuture, (bookings, total) -> {
                    log.info("[4] 합성(thenCombine) thread = {}", Thread.currentThread().getName());
                    return new PageData(bookings, total);
                })
                // (C) bookings에 의존하는 좌석 일괄 조회를 이어서 실행 (thenCompose)
                .thenCompose(pageData -> {
                    List<Long> allSeatIds = pageData.bookings().stream()
                            .flatMap(b -> b.getSeatIds().stream()).distinct().toList();
                    return CompletableFuture.supplyAsync(() -> {
                        log.info("[5] getSeatsByIds(좌석) 처리 thread = {}", Thread.currentThread().getName());
                        Map<Long, Seat> seatById = seatService.getSeatsByIds(allSeatIds).stream()
                                .collect(Collectors.toMap(Seat::getId, s -> s));
                        return buildResponse(pageData, page, size, seatById);
                    }, ioExecutor);
                })
                // (D) 최종 매핑
                .thenApply(body -> {
                    log.info("[6] 응답 조립 thread = {}", Thread.currentThread().getName());
                    return ResponseEntity.ok(body);
                });
    }

    private BookingListResponse buildResponse(PageData pageData, int page, int size, Map<Long, Seat> seatById) {
        int totalPages = (int) Math.ceil((double) pageData.total() / size);
        boolean hasNext = page + 1 < totalPages;

        List<BookingListResponse.BookingItem> items = pageData.bookings().stream()
                .map(b -> mapToItem(b, seatById))
                .toList();

        return new BookingListResponse(items, page, size, pageData.total(), totalPages, hasNext);
    }

    // 병렬 조회(목록 + 개수) 합성 결과를 담는 중간 캐리어
    private record PageData(List<Booking> bookings, long total) {
    }

    // 예매 내역 상세 조회 - Private
    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingDetailResponse> getBooking(
            @AuthUserId Long userId,
            @PathVariable Long bookingId
    ) {
        Booking booking = bookingService.getBookingDetail(bookingId, userId);
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
                booking.getConcertTitle(),
                booking.getVenueName(),
                booking.getScheduleDate().toString(),
                booking.getScheduleTime().toString(),
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

        Booking cancelled = bookingFacade.cancelBooking(bookingId, userId);

        return ResponseEntity.ok(new BookingCancelResponse(
                cancelled.getId(),
                cancelled.getBookingNumber(),
                BookingStatus.valueOf(cancelled.getStatus().name()),
                refundedAmount,
                cancelled.getCancelledAt() != null ? cancelled.getCancelledAt().toString() : LocalDateTime.now().toString()
        ));
    }

    private BookingListResponse.BookingItem mapToItem(Booking booking, Map<Long, Seat> seatById) {
        List<String> seatNumbers = booking.getSeatIds().stream()
                .map(seatById::get)
                .map(Seat::getSeatNumber)
                .toList();

        return new BookingListResponse.BookingItem(
                booking.getId(),
                booking.getConcertTitle(),
                booking.getScheduleDate().toString(),
                booking.getScheduleTime().toString(),
                seatNumbers,
                booking.getTotalAmount(),
                BookingStatus.valueOf(booking.getStatus().name())
        );
    }
}
