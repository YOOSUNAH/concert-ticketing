package com.concertticketing.domain.soldout;

import com.concertticketing.domain.queue.repository.QueueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SoldOutServiceTest {

    @Mock
    QueueRepository queueRepository;

    @InjectMocks
    SoldOutService soldOutService;

    @Test
    void 잔여_좌석이_0이_되면_sold_out_플래그_발행() {
        when(queueRepository.decreaseRemainingSeats(1L, 2)).thenReturn(0L);

        soldOutService.onSeatsTaken(1L, 2);

        verify(queueRepository, times(1)).markSoldOut(1L);
    }

    @Test
    void 잔여_좌석이_음수가_되어도_sold_out_플래그_발행() {
        when(queueRepository.decreaseRemainingSeats(1L, 2)).thenReturn(-1L);

        soldOutService.onSeatsTaken(1L, 2);

        verify(queueRepository, times(1)).markSoldOut(1L);
    }

    @Test
    void 잔여_좌석이_남으면_sold_out_플래그_발행_안_함() {
        when(queueRepository.decreaseRemainingSeats(1L, 2)).thenReturn(4L);

        soldOutService.onSeatsTaken(1L, 2);

        verify(queueRepository, never()).markSoldOut(anyLong());
    }

    @Test
    void 좌석_해제_시_카운터_증가() {
        soldOutService.onSeatsReleased(1L, 2);

        verify(queueRepository, times(1)).increaseRemainingSeats(1L, 2);
    }

    @Test
    void 좌석_해제_시_매진_상태이면_sold_out_플래그_제거() {
        when(queueRepository.isSoldOut(1L)).thenReturn(true);

        soldOutService.onSeatsReleased(1L, 2);

        verify(queueRepository, times(1)).removeSoldOut(1L);
    }

    @Test
    void 좌석_해제_시_매진_아니면_sold_out_플래그_제거_안_함() {
        when(queueRepository.isSoldOut(1L)).thenReturn(false);

        soldOutService.onSeatsReleased(1L, 2);

        verify(queueRepository, never()).removeSoldOut(anyLong());
    }
}
