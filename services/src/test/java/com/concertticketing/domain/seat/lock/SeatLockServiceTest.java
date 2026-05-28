package com.concertticketing.domain.seat.lock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatLockServiceTest {

    @Mock
    SeatLockRepository seatLockRepository;

    @Test
    void 모든_좌석_락_획득_성공() {
        SeatLockService service = new SeatLockService(seatLockRepository, 600);
        when(seatLockRepository.tryAcquire(anyLong(), anyLong(), anyInt())).thenReturn(true);

        service.acquireAll(List.of(1L, 2L, 3L), 100L);

        verify(seatLockRepository, times(3)).tryAcquire(anyLong(), eq(100L), eq(600));
        verify(seatLockRepository, never()).releaseAll(anyCollection());
    }

    @Test
    void 두번째_좌석_락_실패_시_이미_잡은_좌석_롤백() {
        SeatLockService service = new SeatLockService(seatLockRepository, 600);
        when(seatLockRepository.tryAcquire(eq(1L), anyLong(), anyInt())).thenReturn(true);
        when(seatLockRepository.tryAcquire(eq(2L), anyLong(), anyInt())).thenReturn(false);

        assertThatThrownBy(() -> service.acquireAll(List.of(1L, 2L, 3L), 100L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("2");

        // 좌석 1은 잡았으니 롤백 호출
        verify(seatLockRepository, times(1)).releaseAll(List.of(1L));
        // 좌석 3은 시도조차 안 함
        verify(seatLockRepository, never()).tryAcquire(eq(3L), anyLong(), anyInt());
    }

    @Test
    void 첫_좌석_락_실패_시_롤백할_좌석_없음() {
        SeatLockService service = new SeatLockService(seatLockRepository, 600);
        when(seatLockRepository.tryAcquire(eq(1L), anyLong(), anyInt())).thenReturn(false);

        assertThatThrownBy(() -> service.acquireAll(List.of(1L, 2L), 100L))
                .isInstanceOf(IllegalStateException.class);

        verify(seatLockRepository, times(1)).releaseAll(List.of());
        verify(seatLockRepository, never()).tryAcquire(eq(2L), anyLong(), anyInt());
    }
}
