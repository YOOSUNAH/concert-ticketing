package com.concertticketing.domain.booking.service;

import com.concertticketing.domain.booking.entity.Booking;
import com.concertticketing.domain.booking.entity.BookingStatus;
import com.concertticketing.domain.concert.entity.ConcertRef;
import com.concertticketing.domain.concert.service.ConcertRefService;
import com.concertticketing.domain.payment.service.PaymentService;
import com.concertticketing.domain.queue.service.QueueService;
import com.concertticketing.domain.schedule.entity.ScheduleRef;
import com.concertticketing.domain.seat.entity.Seat;
import com.concertticketing.domain.seat.service.SeatService;
import com.concertticketing.domain.soldout.SoldOutService;

import java.util.List;

/**
 * 예매 흐름을 오케스트레이션하는 Facade.
 * 각 서비스를 조합하되, 서비스끼리는 서로를 모르게 한다.
 */
public class BookingFacade {

    private final BookingService bookingService;
    private final QueueService queueService;
    private final SeatService seatService;
    private final ConcertRefService concertRefService;
    private final PaymentService paymentService;
    private final SoldOutService soldOutService;

    public BookingFacade(BookingService bookingService,
                         QueueService queueService,
                         SeatService seatService,
                         ConcertRefService concertRefService,
                         PaymentService paymentService,
                         SoldOutService soldOutService) {
        this.bookingService = bookingService;
        this.queueService = queueService;
        this.seatService = seatService;
        this.concertRefService = concertRefService;
        this.paymentService = paymentService;
        this.soldOutService = soldOutService;
    }

    /**
     * 예매 생성
     * 0. 대기열 통과 검증
     * 1. 좌석 AVAILABLE 검증
     * 2. 1인 최대 예매 수량 검증
     * 3. 좌석 SOLD 처리 + 매진 카운터 감소
     * 4. 예매 생성
     */
    public Booking createBooking(Long userId, Long scheduleId, List<Long> seatIds, String admissionToken) {
        queueService.validateAdmissionToken(userId, scheduleId, admissionToken);

        List<Seat> seats = seatService.getAvailableSeats(seatIds);

        ConcertRef concert = concertRefService.getConcertByScheduleId(scheduleId);
        int alreadyBooked = bookingService.countBookedSeats(userId, scheduleId);
        if (alreadyBooked + seatIds.size() > concert.getMaxTicketsPerPerson()) {
            throw new IllegalStateException("1인 최대 예매 수량을 초과했습니다.");
        }

        seatService.markAllAsSold(seatIds);
        soldOutService.onSeatsTaken(scheduleId, seatIds.size());

        ScheduleRef schedule = concertRefService.getSchedule(scheduleId);
        int totalAmount = seats.stream().mapToInt(Seat::getPrice).sum();
        return bookingService.create(userId, scheduleId, seatIds, totalAmount,
                concert.getTitle(), schedule.getDate(), schedule.getTime(), concert.getVenue());
    }

    /**
     * 예매 실패 처리 (결제 실패 시)
     */
    public Booking failBooking(Long bookingId) {
        Booking booking = bookingService.getBooking(bookingId);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("결제 대기 상태의 예매만 실패 처리할 수 있습니다.");
        }

        booking.cancel();
        seatService.markAllAsAvailable(booking.getSeatIds());
        soldOutService.onSeatsReleased(booking.getScheduleId(), booking.getSeatIds().size());

        return bookingService.save(booking);
    }

    /**
     * 예매 취소
     */
    public Booking cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingService.getBookingDetail(bookingId, userId);

        BookingStatus before = booking.getStatus();
        booking.cancel();

        if (before == BookingStatus.PAID) {
            paymentService.refund(bookingId);
        }

        seatService.markAllAsAvailable(booking.getSeatIds());
        soldOutService.onSeatsReleased(booking.getScheduleId(), booking.getSeatIds().size());

        return bookingService.save(booking);
    }
}
