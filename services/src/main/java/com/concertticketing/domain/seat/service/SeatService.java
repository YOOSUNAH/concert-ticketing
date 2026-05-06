package com.concertticketing.domain.seat.service;

import com.concertticketing.domain.seat.entity.Seat;
import com.concertticketing.domain.seat.entity.SeatStatus;
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

    /**
     * 예매 가능한 좌석 조회
     * - 모든 좌석이 존재하고 AVAILABLE 상태일 때만 반환
     * - 존재하지 않거나 SOLD 좌석이 섞여있으면 예외
     */
    public List<Seat> getAvailableSeats(List<Long> seatIds) {
        List<Seat> seats = seatRepository.findAllByIds(seatIds);

        if (seats.size() != seatIds.size()) {
            throw new IllegalArgumentException("존재하지 않는 좌석이 포함되어 있습니다.");
        }

        for (Seat seat : seats) {
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new IllegalStateException("이미 판매된 좌석이 포함되어 있습니다: " + seat.getSeatNumber());
            }
        }

        return seats;
    }

    /**
     * 좌석 일괄 SOLD 처리 (예매 확정 시)
     */
    public void markAllAsSold(List<Long> seatIds) {
        List<Seat> seats = seatRepository.findAllByIds(seatIds);
        for (Seat seat : seats) {
            seat.markAsSold();
        }
    }

    /**
     * 좌석 일괄 AVAILABLE 복구 (예매 취소/실패 시)
     */
    public void markAllAsAvailable(List<Long> seatIds) {
        List<Seat> seats = seatRepository.findAllByIds(seatIds);
        for (Seat seat : seats) {
            seat.markAsAvailable();
        }
    }
}
