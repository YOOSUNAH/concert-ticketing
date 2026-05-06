package com.concertticketing.domain.seat.repository;

import com.concertticketing.domain.seat.entity.Seat;

import java.util.List;
import java.util.Optional;

public interface SeatRepository {

    List<Seat> findByScheduleId(Long scheduleId);

    Optional<Seat> findById(Long seatId);

    List<Seat> findAllByIds(List<Long> seatIds);
}
