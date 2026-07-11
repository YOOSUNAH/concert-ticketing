package com.concertticketing.domain.seat.service;

import com.concertticketing.domain.seat.entity.Seat;
import com.concertticketing.domain.seat.entity.SeatStatus;
import com.concertticketing.domain.seat.repository.SeatRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatServiceTest {

    @Mock
    SeatRepository seatRepository;

    @InjectMocks
    SeatService seatService;

    // === getAvailableSeats ===

    @Test
    @DisplayName("예매 가능 좌석 조회 성공 - 모든 좌석 AVAILABLE")
    void getAvailableSeats_success() {
        List<Long> seatIds = List.of(101L, 102L);
        Seat seat1 = new Seat(1L, "A-1", "VIP", 121000);
        Seat seat2 = new Seat(1L, "A-2", "VIP", 121000);
        when(seatRepository.findAllByIds(seatIds)).thenReturn(List.of(seat1, seat2));

        List<Seat> result = seatService.getAvailableSeats(seatIds);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("예매 가능 좌석 조회 실패 - 존재하지 않는 좌석 포함")
    void getAvailableSeats_missingSeat_throwsException() {
        List<Long> seatIds = List.of(101L, 102L);
        Seat seat1 = new Seat(1L, "A-1", "VIP", 121000);
        when(seatRepository.findAllByIds(seatIds)).thenReturn(List.of(seat1)); // 1개만 반환

        assertThrows(IllegalArgumentException.class,
                () -> seatService.getAvailableSeats(seatIds));
    }

    @Test
    @DisplayName("예매 가능 좌석 조회 실패 - SOLD 좌석 포함")
    void getAvailableSeats_soldSeat_throwsException() {
        List<Long> seatIds = List.of(101L);
        Seat sold = new Seat(1L, "A-1", "VIP", 121000);
        sold.markAsSold();
        when(seatRepository.findAllByIds(seatIds)).thenReturn(List.of(sold));

        assertThrows(IllegalStateException.class,
                () -> seatService.getAvailableSeats(seatIds));
    }

    // === markAllAsSold / markAllAsAvailable ===

    @Test
    @DisplayName("좌석 일괄 SOLD 처리 - 모든 좌석 AVAILABLE이면 성공")
    void markAllAsSold_allAvailable_success() {
        List<Long> seatIds = List.of(101L, 102L);
        when(seatRepository.markAsSoldWhereAvailable(seatIds)).thenReturn(2);

        seatService.markAllAsSold(seatIds);

        verify(seatRepository).markAsSoldWhereAvailable(seatIds);
    }

    @Test
    @DisplayName("좌석 일괄 SOLD 처리 - 이미 판매된 좌석 포함 시 실패")
    void markAllAsSold_someAlreadySold_throwsException() {
        List<Long> seatIds = List.of(101L, 102L);
        when(seatRepository.markAsSoldWhereAvailable(seatIds)).thenReturn(1); // 1개만 변경됨

        assertThrows(IllegalStateException.class,
                () -> seatService.markAllAsSold(seatIds));
    }

    @Test
    @DisplayName("좌석 일괄 AVAILABLE 복구")
    void markAllAsAvailable_success() {
        List<Long> seatIds = List.of(101L, 102L);

        seatService.markAllAsAvailable(seatIds);

        verify(seatRepository).markAsAvailableWhereSold(seatIds);
    }
}
