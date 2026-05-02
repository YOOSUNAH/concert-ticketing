package com.concertticketing.domain.seat.service;

import com.concertticketing.domain.seat.entity.Seat;
import com.concertticketing.domain.seat.repository.SeatRepository;

import java.util.List;

public class SeatService {

    private final SeatRepository seatRepository;

    public SeatService(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    /**
     * 좌석 조회
     * - 해당 스케줄의 전체 좌석 목록 반환 (AVAILABLE + SOLD 모두)
     * - 프론트에서 status 값으로 SOLD 좌석은 회색 처리, AVAILABLE은 선택 가능하게 표시
     */
    public List<Seat> getSeats(Long scheduleId) {
        return seatRepository.findByScheduleId(scheduleId);
    }
}
