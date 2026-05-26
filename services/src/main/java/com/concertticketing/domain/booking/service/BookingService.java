package com.concertticketing.domain.booking.service;

import com.concertticketing.domain.booking.entity.Booking;
import com.concertticketing.domain.booking.entity.BookingStatus;
import com.concertticketing.domain.booking.repository.BookingRepository;
import com.concertticketing.domain.concert.entity.ConcertRef;
import com.concertticketing.domain.concert.service.ConcertRefService;
import com.concertticketing.domain.payment.service.PaymentService;
import com.concertticketing.domain.queue.service.QueueService;
import com.concertticketing.domain.schedule.entity.ScheduleRef;
import com.concertticketing.domain.seat.entity.Seat;
import com.concertticketing.domain.seat.service.SeatService;
import com.concertticketing.domain.soldout.SoldOutService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BookingService {

    private final BookingRepository bookingRepository;
    private final SeatService seatService;
    private final ConcertRefService concertRefService;
    private final PaymentService paymentService;
    private final QueueService queueService;
    private final SoldOutService soldOutService;

    public BookingService(BookingRepository bookingRepository,
                          SeatService seatService,
                          ConcertRefService concertRefService,
                          PaymentService paymentService,
                          QueueService queueService,
                          SoldOutService soldOutService) {
        this.bookingRepository = bookingRepository;
        this.seatService = seatService;
        this.concertRefService = concertRefService;
        this.paymentService = paymentService;
        this.queueService = queueService;
        this.soldOutService = soldOutService;
    }

    /**
     * 예매 생성
     * 0. 대기열 통과 검증 (admissionToken)
     * 1. 좌석 AVAILABLE 검증 (SeatService)
     * 2. 1인 최대 예매 수량 초과 검증 (ConcertService로 schedule→concert 흡수)
     * 3. 좌석 SOLD 처리 (DB 조건부 UPDATE로 동시성 보장) + 잔여 좌석 카운터 감소
     * 4. 예매 생성 & 저장
     */
    public Booking createBooking(Long userId, Long scheduleId, List<Long> seatIds, String admissionToken) {
        // 0. 대기열 통과 검증
        queueService.validateAdmissionToken(userId, scheduleId, admissionToken);

        // 1. 좌석 조회 + AVAILABLE 검증
        List<Seat> seats = seatService.getAvailableSeats(seatIds);

        // 2. 1인 최대 예매 수량 검증
        ConcertRef concert = concertRefService.getConcertByScheduleId(scheduleId);
        int alreadyBooked = bookingRepository.countSeatsByUserIdAndScheduleId(userId, scheduleId);
        if (alreadyBooked + seatIds.size() > concert.getMaxTicketsPerPerson()) {
            throw new IllegalStateException("1인 최대 예매 수량을 초과했습니다.");
        }

        // 3. 좌석 SOLD 처리 (DB 조건부 UPDATE로 동시성 보장) + 잔여 좌석 카운터 감소
        seatService.markAllAsSold(seatIds);
        soldOutService.onSeatsTaken(scheduleId, seatIds.size());

        // 4. 예매 생성 (Concert/Schedule 정보 비정규화로 함께 저장)
        ScheduleRef schedule = concertRefService.getSchedule(scheduleId);
        int totalAmount = seats.stream().mapToInt(Seat::getPrice).sum();
        Booking booking = new Booking(
                userId, scheduleId, generateBookingNumber(), seatIds, totalAmount,
                concert.getTitle(), schedule.getDate(), schedule.getTime(), concert.getVenue()
        );
        return bookingRepository.save(booking);
    }

    /**
     * 예매 내역 조회 (내 예매 목록)
     */
    public List<Booking> getMyBookings(Long userId, int page, int size) {
        return bookingRepository.findByUserId(userId, page, size);
    }

    /**
     * 예매 내역 총 개수 (페이징 정보용)
     */
    public long getMyBookingsCount(Long userId) {
        return bookingRepository.countByUserId(userId);
    }

    /**
     * 예매 상세 조회
     */
    public Booking getBookingDetail(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예매입니다."));

        if (!booking.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 예매만 조회할 수 있습니다.");
        }

        return booking;
    }

    /**
     * 예매 실패 처리 (결제 실패 시 호출, 본인 검증 없음)
     * - PENDING 상태에서만 호출 가능
     * 1. 예매 상태를 CANCELLED로 변경
     * 2. 좌석 복구 (SeatService)
     */
    public Booking failBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예매입니다."));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("결제 대기 상태의 예매만 실패 처리할 수 있습니다.");
        }

        booking.cancel();
        seatService.markAllAsAvailable(booking.getSeatIds());
        soldOutService.onSeatsReleased(booking.getScheduleId(), booking.getSeatIds().size());

        return bookingRepository.save(booking);
    }

    /**
     * 예매 취소 (전체 좌석 일괄 취소)
     * 1. 예매 상태를 CANCELLED로 변경
     * 2. PAID 상태였다면 결제 환불 처리
     * 3. 좌석 일괄 AVAILABLE 복구 (SeatService)
     */
    public Booking cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예매입니다."));

        if (!booking.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 예매만 취소할 수 있습니다.");
        }

        // 1. 예매 취소 (상태 변경 전 상태 보존)
        BookingStatus before = booking.getStatus();
        booking.cancel();

        // 2. 결제 완료 상태였다면 환불
        if (before == BookingStatus.PAID) {
            paymentService.refund(bookingId);
        }

        // 3. 좌석 복구 + 잔여 좌석 카운터 증가
        seatService.markAllAsAvailable(booking.getSeatIds());
        soldOutService.onSeatsReleased(booking.getScheduleId(), booking.getSeatIds().size());

        return bookingRepository.save(booking);
    }

    private String generateBookingNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long sequence = System.nanoTime() % 1000;
        return "BK" + date + String.format("%03d", sequence);
    }
}
