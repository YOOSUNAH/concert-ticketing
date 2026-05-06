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
    @DisplayName("좌석 일괄 SOLD 처리")
    void markAllAsSold_changesAllToSold() {
        List<Long> seatIds = List.of(101L, 102L);
        Seat seat1 = new Seat(1L, "A-1", "VIP", 121000);
        Seat seat2 = new Seat(1L, "A-2", "VIP", 121000);
        when(seatRepository.findAllByIds(seatIds)).thenReturn(List.of(seat1, seat2));

        seatService.markAllAsSold(seatIds);

        assertEquals(SeatStatus.SOLD, seat1.getStatus());
        assertEquals(SeatStatus.SOLD, seat2.getStatus());
    }

    @Test
    @DisplayName("좌석 일괄 AVAILABLE 복구")
    void markAllAsAvailable_changesAllToAvailable() {
        List<Long> seatIds = List.of(101L, 102L);
        Seat seat1 = new Seat(1L, "A-1", "VIP", 121000);
        Seat seat2 = new Seat(1L, "A-2", "VIP", 121000);
        seat1.markAsSold();
        seat2.markAsSold();
        when(seatRepository.findAllByIds(seatIds)).thenReturn(List.of(seat1, seat2));

        seatService.markAllAsAvailable(seatIds);

        assertEquals(SeatStatus.AVAILABLE, seat1.getStatus());
        assertEquals(SeatStatus.AVAILABLE, seat2.getStatus());
    }
}
