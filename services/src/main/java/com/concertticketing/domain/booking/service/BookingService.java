package com.concertticketing.domain.booking.service;

import com.concertticketing.domain.booking.entity.Booking;
import com.concertticketing.domain.booking.entity.BookingStatus;
import com.concertticketing.domain.booking.repository.BookingRepository;
import com.concertticketing.domain.concert.entity.Concert;
import com.concertticketing.domain.concert.repository.ConcertRepository;
import com.concertticketing.domain.seat.entity.Seat;
import com.concertticketing.domain.seat.entity.SeatStatus;
import com.concertticketing.domain.seat.repository.SeatRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BookingService {

    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final ConcertRepository concertRepository;

    public BookingService(BookingRepository bookingRepository,
                          SeatRepository seatRepository,
                          ConcertRepository concertRepository) {
        this.bookingRepository = bookingRepository;
        this.seatRepository = seatRepository;
        this.concertRepository = concertRepository;
    }

    /**
     * 예매 생성
     * 1. 좌석이 모두 AVAILABLE인지 확인
     * 2. 1인 최대 예매 수량 초과 확인
     * 3. 좌석 상태를 SOLD로 변경
     * 4. 예매 생성 & 저장
     */
    public Booking createBooking(Long userId, Long scheduleId, List<Long> seatIds) {
        // 1. 좌석 조회 & AVAILABLE 확인
        List<Seat> seats = seatRepository.findAllByIds(seatIds);

        if (seats.size() != seatIds.size()) {
            throw new IllegalArgumentException("존재하지 않는 좌석이 포함되어 있습니다.");
        }

        for (Seat seat : seats) {
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new IllegalStateException("이미 판매된 좌석이 포함되어 있습니다: " + seat.getSeatNumber());
            }
        }

        // 2. 1인 최대 예매 수량 확인
        Concert concert = concertRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 콘서트입니다."));

        int alreadyBooked = bookingRepository.countSeatsByUserIdAndScheduleId(userId, scheduleId);
        if (alreadyBooked + seatIds.size() > concert.getMaxTicketsPerPerson()) {
            throw new IllegalStateException("1인 최대 예매 수량을 초과했습니다.");
        }

        // 3. 좌석 상태를 SOLD로 변경
        for (Seat seat : seats) {
            seat.markAsSold();
        }

        // 4. 예매 생성
        int totalAmount = seats.stream().mapToInt(Seat::getPrice).sum();
        String bookingNumber = generateBookingNumber();

        Booking booking = new Booking(userId, scheduleId, bookingNumber, seatIds, totalAmount);
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
     * 예매 취소
     * 1. 예매 상태를 CANCELLED로 변경
     * 2. 좌석 상태를 AVAILABLE로 복구
     */
    public Booking cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예매입니다."));

        if (!booking.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 예매만 취소할 수 있습니다.");
        }

        // 1. 예매 취소
        booking.cancel();

        // 2. 좌석 복구
        List<Seat> seats = seatRepository.findAllByIds(booking.getSeatIds());
        for (Seat seat : seats) {
            seat.markAsAvailable();
        }

        return bookingRepository.save(booking);
    }

    private String generateBookingNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long sequence = System.nanoTime() % 1000;
        return "BK" + date + String.format("%03d", sequence);
    }
}
