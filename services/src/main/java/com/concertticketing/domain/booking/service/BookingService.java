package com.concertticketing.domain.booking.service;

import com.concertticketing.domain.booking.entity.Booking;
import com.concertticketing.domain.booking.repository.BookingRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 예매 도메인 서비스 — 예매 CRUD만 담당.
 * 다른 서비스를 의존하지 않는다.
 */
public class BookingService {

    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public Booking create(Long userId, Long scheduleId, List<Long> seatIds, int totalAmount,
                          String concertTitle, LocalDate scheduleDate, LocalTime scheduleTime, String venue) {
        Booking booking = new Booking(
                userId, scheduleId, generateBookingNumber(), seatIds, totalAmount,
                concertTitle, scheduleDate, scheduleTime, venue
        );
        return bookingRepository.save(booking);
    }

    public Booking save(Booking booking) {
        return bookingRepository.save(booking);
    }

    public Booking getBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예매입니다."));
    }

    public Booking getBookingDetail(Long bookingId, Long userId) {
        Booking booking = getBooking(bookingId);
        if (!booking.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 예매만 조회할 수 있습니다.");
        }
        return booking;
    }

    public List<Booking> getMyBookings(Long userId, int page, int size) {
        return bookingRepository.findByUserId(userId, page, size);
    }

    public long getMyBookingsCount(Long userId) {
        return bookingRepository.countByUserId(userId);
    }

    public int countBookedSeats(Long userId, Long scheduleId) {
        return bookingRepository.countSeatsByUserIdAndScheduleId(userId, scheduleId);
    }

    private String generateBookingNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long sequence = System.nanoTime() % 1000;
        return "BK" + date + String.format("%03d", sequence);
    }
}
