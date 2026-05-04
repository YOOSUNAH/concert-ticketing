package com.concertticketing.domain.booking.repository;

import com.concertticketing.domain.booking.entity.Booking;

import java.util.List;
import java.util.Optional;

public interface BookingRepository {

    Booking save(Booking booking);

    Optional<Booking> findById(Long bookingId);

    List<Booking> findByUserId(Long userId, int page, int size);

    long countByUserId(Long userId);

    /**
     * 해당 스케줄에서 사용자가 활성 상태로 보유한 Booking 개수 (PENDING + PAID, CANCELLED 제외)
     * - 1Booking = 1매 모델이므로 이 개수가 곧 보유 매수
     * - 예매 후 취소하면 카운트에서 빠져야 재예매가 가능함
     * - 구현 시 WHERE status != 'CANCELLED' 조건 필수
     */
    int countActiveByUserIdAndScheduleId(Long userId, Long scheduleId);
}
