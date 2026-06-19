package com.concertticketing.domain.booking.service;

import com.concertticketing.domain.booking.entity.Booking;
import com.concertticketing.domain.booking.entity.BookingStatus;
import com.concertticketing.domain.concert.entity.ConcertRef;
import com.concertticketing.domain.concert.entity.ConcertStatus;
import com.concertticketing.domain.concert.service.ConcertRefService;
import com.concertticketing.domain.payment.service.PaymentService;
import com.concertticketing.domain.queue.service.QueueService;
import com.concertticketing.domain.schedule.entity.ScheduleRef;
import com.concertticketing.domain.seat.entity.Seat;
import com.concertticketing.domain.seat.service.SeatService;
import com.concertticketing.domain.soldout.SoldOutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingFacadeTest {

    @Mock BookingService bookingService;
    @Mock QueueService queueService;
    @Mock SeatService seatService;
    @Mock ConcertRefService concertRefService;
    @Mock PaymentService paymentService;
    @Mock SoldOutService soldOutService;

    BookingFacade sut;

    private static final String VALID_TOKEN = "1:1:admitted";

    @BeforeEach
    void setUp() {
        sut = new BookingFacade(bookingService, queueService, seatService,
                concertRefService, paymentService, soldOutService);
    }

    // === 예매 생성 ===

    @Test
    @DisplayName("예매 성공 — 전체 흐름 오케스트레이션")
    void createBooking_success() {
        Long userId = 1L;
        Long scheduleId = 1L;
        List<Long> seatIds = List.of(101L, 102L);

        Seat seat1 = new Seat(scheduleId, "A-1", "VIP", 121000);
        Seat seat2 = new Seat(scheduleId, "A-2", "VIP", 121000);
        when(seatService.getAvailableSeats(seatIds)).thenReturn(List.of(seat1, seat2));

        ConcertRef concert = new ConcertRef("10cm 콘서트", "올림픽공원", 4, ConcertStatus.OPEN);
        when(concertRefService.getConcertByScheduleId(scheduleId)).thenReturn(concert);

        ScheduleRef schedule = stubScheduleRef(1L, 1L);
        when(concertRefService.getSchedule(scheduleId)).thenReturn(schedule);

        when(bookingService.countBookedSeats(userId, scheduleId)).thenReturn(0);

        Booking savedBooking = new Booking(userId, scheduleId, "BK20260801001", seatIds, 242000,
                "10cm 콘서트", LocalDate.of(2026, 8, 1), LocalTime.of(19, 0), "올림픽공원");
        when(bookingService.create(anyLong(), anyLong(), anyList(), anyInt(),
                anyString(), any(), any(), anyString())).thenReturn(savedBooking);

        Booking result = sut.createBooking(userId, scheduleId, seatIds, VALID_TOKEN);

        assertThat(result.getTotalAmount()).isEqualTo(242000);
        verify(queueService).validateAdmissionToken(userId, scheduleId, VALID_TOKEN);
        verify(seatService).markAllAsSold(seatIds);
        verify(soldOutService).onSeatsTaken(scheduleId, 2);
        verify(bookingService).create(anyLong(), anyLong(), anyList(), anyInt(),
                anyString(), any(), any(), anyString());
    }

    @Test
    @DisplayName("예매 실패 — 대기열 미통과")
    void createBooking_invalidToken() {
        doThrow(new IllegalArgumentException("대기열 입장 권한이 없습니다."))
                .when(queueService).validateAdmissionToken(anyLong(), anyLong(), anyString());

        assertThatThrownBy(() -> sut.createBooking(1L, 1L, List.of(101L), "invalid"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(seatService, never()).getAvailableSeats(anyList());
    }

    @Test
    @DisplayName("예매 실패 — 1인 최대 수량 초과")
    void createBooking_exceedsMax() {
        when(seatService.getAvailableSeats(anyList())).thenReturn(
                List.of(new Seat(1L, "A-1", "VIP", 121000), new Seat(1L, "A-2", "VIP", 121000)));
        ConcertRef concert = new ConcertRef("콘서트", "장소", 2, ConcertStatus.OPEN);
        when(concertRefService.getConcertByScheduleId(1L)).thenReturn(concert);
        when(bookingService.countBookedSeats(1L, 1L)).thenReturn(1);

        assertThatThrownBy(() -> sut.createBooking(1L, 1L, List.of(101L, 102L), VALID_TOKEN))
                .isInstanceOf(IllegalStateException.class);

        verify(seatService, never()).markAllAsSold(anyList());
    }

    // === 예매 실패 처리 ===

    @Test
    @DisplayName("예매 실패 처리 — PENDING 상태에서 좌석 복구")
    void failBooking_success() {
        Booking booking = createBooking(BookingStatus.PENDING);
        when(bookingService.getBooking(999L)).thenReturn(booking);
        when(bookingService.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Booking result = sut.failBooking(999L);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(seatService).markAllAsAvailable(booking.getSeatIds());
        verify(soldOutService).onSeatsReleased(eq(1L), eq(2));
    }

    @Test
    @DisplayName("예매 실패 처리 — PAID 상태면 예외")
    void failBooking_paidThrows() {
        Booking booking = createBooking(BookingStatus.PENDING);
        booking.markAsPaid();
        when(bookingService.getBooking(999L)).thenReturn(booking);

        assertThatThrownBy(() -> sut.failBooking(999L))
                .isInstanceOf(IllegalStateException.class);
    }

    // === 예매 취소 ===

    @Test
    @DisplayName("예매 취소 — PENDING 상태 (환불 없음)")
    void cancelBooking_pending() {
        Booking booking = createBooking(BookingStatus.PENDING);
        when(bookingService.getBookingDetail(999L, 1L)).thenReturn(booking);
        when(bookingService.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Booking result = sut.cancelBooking(999L, 1L);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(paymentService, never()).refund(anyLong());
        verify(seatService).markAllAsAvailable(booking.getSeatIds());
    }

    @Test
    @DisplayName("예매 취소 — PAID 상태 (환불 호출)")
    void cancelBooking_paid() {
        Booking booking = createBooking(BookingStatus.PENDING);
        booking.markAsPaid();
        when(bookingService.getBookingDetail(999L, 1L)).thenReturn(booking);
        when(bookingService.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Booking result = sut.cancelBooking(999L, 1L);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(paymentService).refund(999L);
    }

    // === helpers ===

    private Booking createBooking(BookingStatus status) {
        return new Booking(1L, 1L, "BK20260801001", List.of(101L, 102L), 242000,
                "10cm 콘서트", LocalDate.of(2026, 8, 1), LocalTime.of(19, 0), "올림픽공원");
    }

    private ScheduleRef stubScheduleRef(Long id, Long concertId) {
        ScheduleRef ref = new ScheduleRef(concertId, LocalDate.of(2026, 8, 1), LocalTime.of(19, 0), 500);
        try {
            var field = ScheduleRef.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(ref, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ref;
    }
}
