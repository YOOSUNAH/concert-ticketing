package com.concertticketing.domain.soldout;

import com.concertticketing.domain.queue.repository.QueueRepository;

public class SoldOutService {

    private final QueueRepository queueRepository;

    public SoldOutService(QueueRepository queueRepository) {
        this.queueRepository = queueRepository;
    }

    public void initRemainingSeats(Long scheduleId, int total, java.time.LocalDate scheduleDate) {
        queueRepository.initRemainingSeats(scheduleId, total, scheduleDate);
    }

    /**
     * 좌석 점유 시 호출.
     * 잔여가 0 이하가 되면 sold_out 플래그를 발행.
     */
    public void onSeatsTaken(Long scheduleId, int count) {
        long remaining = queueRepository.decreaseRemainingSeats(scheduleId, count);
        if (remaining <= 0) {
            queueRepository.markSoldOut(scheduleId);
        }
    }

    /**
     * 좌석 해제 시 호출 (예매 취소/실패).
     * 잔여석이 다시 생기면 sold_out 플래그를 제거하여 예매를 재개한다.
     */
    public void onSeatsReleased(Long scheduleId, int count) {
        queueRepository.increaseRemainingSeats(scheduleId, count);
        if (queueRepository.isSoldOut(scheduleId)) {
            queueRepository.removeSoldOut(scheduleId);
        }
    }
}
