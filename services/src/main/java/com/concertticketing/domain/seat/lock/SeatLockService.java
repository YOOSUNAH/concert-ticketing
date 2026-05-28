package com.concertticketing.domain.seat.lock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SeatLockService {

    private final SeatLockRepository seatLockRepository;
    private final int ttlSeconds;

    public SeatLockService(SeatLockRepository seatLockRepository, int ttlSeconds) {
        this.seatLockRepository = seatLockRepository;
        this.ttlSeconds = ttlSeconds;
    }

    /**
     * 여러 좌석에 일괄 락 획득.
     * 일부 좌석 락 획득 실패 시, 이미 잡은 좌석들을 모두 해제 후 예외를 던진다.
     */
    public void acquireAll(List<Long> seatIds, Long userId) {
        List<Long> acquired = new ArrayList<>();
        for (Long seatId : seatIds) {
            boolean ok = seatLockRepository.tryAcquire(seatId, userId, ttlSeconds);
            if (!ok) {
                seatLockRepository.releaseAll(acquired);
                throw new IllegalStateException("좌석이 이미 점유 중입니다: " + seatId);
            }
            acquired.add(seatId);
        }
    }

    public void releaseAll(Collection<Long> seatIds) {
        seatLockRepository.releaseAll(seatIds);
    }
}
