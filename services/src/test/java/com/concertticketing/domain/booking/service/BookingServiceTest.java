package com.concertticketing.domain.booking.service;

import com.concertticketing.domain.booking.entity.Booking;
import com.concertticketing.domain.booking.entity.BookingStatus;
import com.concertticketing.domain.booking.repository.BookingRepository;
import com.concertticketing.domain.concert.entity.Concert;
import com.concertticketing.domain.concert.entity.ConcertStatus;
import com.concertticketing.domain.concert.service.ConcertService;
import com.concertticketing.domain.payment.service.PaymentService;
import com.concertticketing.domain.queue.service.QueueService;
import com.concertticketing.domain.schedule.entity.Schedule;
import com.concertticketing.domain.seat.entity.Seat;
import com.concertticketing.domain.seat.lock.SeatLockService;
import com.concertticketing.domain.seat.service.SeatService;
import com.concertticketing.domain.soldout.SoldOutService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    BookingRepository bookingRepository;

    @Mock
    SeatService seatService;

    @Mock
    ConcertService concertService;

    @Mock
    PaymentService paymentService;

    @Mock
    QueueService queueService;

    @Mock
    SoldOutService soldOutService;

    @Mock
    SeatLockService seatLockService;

    @InjectMocks
    BookingService bookingService;

    private static final String VALID_TOKEN = "1:1:admitted";

    // === 예매 생성 테스트 ===

    @Test
    @DisplayName("예매 성공 - 좌석 AVAILABLE + 수량 미초과")
    void createBooking_success() {
        // given
        Long userId = 1L;
        Long scheduleId = 1L;
        List<Long> seatIds = List.of(101L, 102L);

        Seat seat1 = new Seat(scheduleId, "A-1", "VIP", 121000);
        Seat seat2 = new Seat(scheduleId, "A-2", "VIP", 121000);
        when(seatService.getAvailableSeats(seatIds)).thenReturn(List.of(seat1, seat2));

        Concert concert = new Concert(1L, "10cm 콘서트", "10cm", "설명", "올림픽공원",
                null, null, LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 2),
                4, ConcertStatus.OPEN);
        when(concertService.getConcertByScheduleId(scheduleId)).thenReturn(concert);

        Schedule schedule = new Schedule(1L, 1L, LocalDate.of(2025, 8, 1), LocalTime.of(19, 0), 500, 380);
        when(concertService.getSchedule(scheduleId)).thenReturn(schedule);

        when(bookingRepository.countSeatsByUserIdAndScheduleId(userId, scheduleId)).thenReturn(0);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Booking booking = bookingService.createBooking(userId, scheduleId, seatIds, VALID_TOKEN);

        // then
        assertEquals(242000, booking.getTotalAmount());           // 121000 × 2
        assertEquals(BookingStatus.PENDING, booking.getStatus()); // 초기 상태는 PENDING
        verify(queueService, times(1)).validateAdmissionToken(userId, scheduleId, VALID_TOKEN);
        verify(seatService, times(1)).markAllAsSold(seatIds);     // 좌석 SOLD 처리 위임 검증
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("예매 실패 - admissionToken 검증 실패 (대기열 미통과)")
    void createBooking_invalidAdmissionToken_throwsException() {
        // given - QueueService가 검증 실패로 예외를 던짐
        Long userId = 1L;
        Long scheduleId = 1L;
        List<Long> seatIds = List.of(101L);

        doThrow(new IllegalArgumentException("대기열 입장 권한이 만료되었거나 없습니다."))
                .when(queueService).validateAdmissionToken(anyLong(), anyLong(), anyString());

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(userId, scheduleId, seatIds, "invalid-token"));

        // 검증 실패 시 좌석 조회/저장 등 후속 단계는 도달 안 함
        verify(seatService, never()).getAvailableSeats(anyList());
        verify(seatService, never()).markAllAsSold(anyList());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("예매 실패 - 이미 판매된 좌석이 포함됨")
    void createBooking_soldSeat_throwsException() {
        // given - SeatService가 SOLD 좌석 검증 실패로 예외
        Long userId = 1L;
        Long scheduleId = 1L;
        List<Long> seatIds = List.of(101L);

        when(seatService.getAvailableSeats(seatIds))
                .thenThrow(new IllegalStateException("이미 판매된 좌석이 포함되어 있습니다: A-1"));

        // when & then
        assertThrows(IllegalStateException.class,
                () -> bookingService.createBooking(userId, scheduleId, seatIds, VALID_TOKEN));

        verify(seatService, never()).markAllAsSold(anyList());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("예매 실패 - 1인 최대 예매 수량 초과")
    void createBooking_exceedsMaxTickets_throwsException() {
        // given
        Long userId = 1L;
        Long scheduleId = 1L;
        List<Long> seatIds = List.of(101L, 102L);

        Seat seat1 = new Seat(scheduleId, "A-1", "VIP", 121000);
        Seat seat2 = new Seat(scheduleId, "A-2", "VIP", 121000);
        when(seatService.getAvailableSeats(seatIds)).thenReturn(List.of(seat1, seat2));

        // maxTicketsPerPerson = 2인데, 이미 1매 예매함 → 2매 추가하면 총 3매 → 초과
        Concert concert = new Concert(1L, "10cm 콘서트", "10cm", "설명", "올림픽공원",
                null, null, LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 2),
                2, ConcertStatus.OPEN);
        when(concertService.getConcertByScheduleId(scheduleId)).thenReturn(concert);

        when(bookingRepository.countSeatsByUserIdAndScheduleId(userId, scheduleId)).thenReturn(1);

        // when & then
        assertThrows(IllegalStateException.class,
                () -> bookingService.createBooking(userId, scheduleId, seatIds, VALID_TOKEN));

        verify(seatService, never()).markAllAsSold(anyList());
        verify(bookingRepository, never()).save(any());
    }

    // === 예매 취소 테스트 ===

    @Test
    @DisplayName("예매 취소 성공 (PENDING) - 환불 호출 없이 좌석 일괄 복구")
    void cancelBooking_pending_success() {
        // given - PENDING 상태 booking
        Long bookingId = 999L;
        Long userId = 1L;
        List<Long> seatIds = List.of(101L, 102L);

        Booking booking = new Booking(userId, 1L, "BK20250801001", seatIds, 242000,
                "10cm 콘서트", LocalDate.of(2025, 8, 1), LocalTime.of(19, 0), "올림픽공원");
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Booking cancelled = bookingService.cancelBooking(bookingId, userId);

        // then
        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());     // 예매 취소됨
        verify(seatService, times(1)).markAllAsAvailable(seatIds);        // 좌석 복구 위임
        verify(paymentService, never()).refund(anyLong());                // 환불 호출 없음
    }

    @Test
    @DisplayName("예매 취소 성공 (PAID) - 환불 호출 + 좌석 일괄 복구")
    void cancelBooking_paid_callsRefund() {
        // given - PAID 상태 booking
        Long bookingId = 999L;
        Long userId = 1L;
        List<Long> seatIds = List.of(101L, 102L);

        Booking booking = new Booking(userId, 1L, "BK20250801001", seatIds, 242000,
                "10cm 콘서트", LocalDate.of(2025, 8, 1), LocalTime.of(19, 0), "올림픽공원");
        booking.markAsPaid(); // PAID 상태로 만듦
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Booking cancelled = bookingService.cancelBooking(bookingId, userId);

        // then
        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());
        verify(paymentService, times(1)).refund(bookingId);               // 환불 1회 호출
        verify(seatService, times(1)).markAllAsAvailable(seatIds);
    }

    @Test
    @DisplayName("예매 취소 실패 - 본인의 예매가 아님")
    void cancelBooking_notOwner_throwsException() {
        // given
        Long bookingId = 999L;
        Long ownerUserId = 1L;
        Long otherUserId = 2L;

        Booking booking = new Booking(ownerUserId, 1L, "BK20250801001", List.of(101L), 121000,
                "10cm 콘서트", LocalDate.of(2025, 8, 1), LocalTime.of(19, 0), "올림픽공원");
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        // when & then - 다른 사람이 취소 시도
        assertThrows(IllegalArgumentException.class,
                () -> bookingService.cancelBooking(bookingId, otherUserId));

        verify(bookingRepository, never()).save(any());
        verify(seatService, never()).markAllAsAvailable(anyList());
        verify(paymentService, never()).refund(anyLong());
    }

    // === 예매 실패 처리 테스트 (결제 실패 시 보상) ===

    @Test
    @DisplayName("예매 실패 처리 성공 - PENDING booking을 CANCELLED로 + 좌석 일괄 복구")
    void failBooking_success() {
        // given - PENDING 상태 booking
        Long bookingId = 999L;
        List<Long> seatIds = List.of(101L, 102L);

        Booking booking = new Booking(1L, 1L, "BK20250801001", seatIds, 242000,
                "10cm 콘서트", LocalDate.of(2025, 8, 1), LocalTime.of(19, 0), "올림픽공원");
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Booking failed = bookingService.failBooking(bookingId);

        // then
        assertEquals(BookingStatus.CANCELLED, failed.getStatus());
        verify(seatService, times(1)).markAllAsAvailable(seatIds);
        verify(paymentService, never()).refund(anyLong()); // payment 미존재 → 환불 호출 없음
    }

    @Test
    @DisplayName("예매 실패 처리 실패 - 이미 PAID 상태인 booking")
    void failBooking_alreadyPaid_throwsException() {
        // given - PAID booking
        Long bookingId = 999L;
        Booking booking = new Booking(1L, 1L, "BK20250801001", List.of(101L), 121000,
                "10cm 콘서트", LocalDate.of(2025, 8, 1), LocalTime.of(19, 0), "올림픽공원");
        booking.markAsPaid();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        // when & then
        assertThrows(IllegalStateException.class,
                () -> bookingService.failBooking(bookingId));

        verify(bookingRepository, never()).save(any());
        verify(seatService, never()).markAllAsAvailable(anyList());
    }

    @Test
    @DisplayName("예매 실패 처리 실패 - booking 없음")
    void failBooking_notFound_throwsException() {
        // given
        Long bookingId = 999L;
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> bookingService.failBooking(bookingId));

        verify(bookingRepository, never()).save(any());
    }
}
