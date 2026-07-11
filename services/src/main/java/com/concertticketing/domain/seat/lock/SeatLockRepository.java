package com.concertticketing.domain.seat.lock;

import java.util.Collection;

public interface SeatLockRepository {

    /**
     * 좌석에 락을 시도한다.
     * @return true면 락 획득 성공, false면 이미 다른 사용자가 잡고 있음
     */
    boolean tryAcquire(Long seatId, Long userId, int ttlSeconds);

    /** 락 해제 (소유자 검증 없이 단순 DEL). */
    void release(Long seatId);

    /** 여러 좌석 일괄 해제. */
    void releaseAll(Collection<Long> seatIds);
}
