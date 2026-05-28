package com.concertticketing.domain.soldout;

import com.concertticketing.domain.queue.repository.QueueRepository;

public class SoldOutService {

    private static final int SOLD_OUT_TTL_SECONDS = 86400; // 24시간

    private final QueueRepository queueRepository;

    public SoldOutService(QueueRepository queueRepository) {
        this.queueRepository = queueRepository;
    }

    public void initRemainingSeats(Long scheduleId, int total) {
        queueRepository.initRemainingSeats(scheduleId, total);
    }

    /**
     * 좌석 점유 시 호출.
     * 잔여가 0 이하가 되면 sold_out 플래그를 발행.
     */
    public void onSeatsTaken(Long scheduleId, int count) {
        long remaining = queueRepository.decreaseRemainingSeats(scheduleId, count);
        if (remaining <= 0) {
            queueRepository.markSoldOut(scheduleId, SOLD_OUT_TTL_SECONDS);
        }
    }

    /** 좌석 해제 시 호출 (예매 취소/실패). */
    public void onSeatsReleased(Long scheduleId, int count) {
        queueRepository.increaseRemainingSeats(scheduleId, count);
    }
}
