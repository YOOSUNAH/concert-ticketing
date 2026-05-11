package com.concertticketing.domain.seat.repository;

import com.concertticketing.domain.seat.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataSeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByScheduleIdOrderByIdAsc(Long scheduleId);

    List<Seat> findByIdIn(List<Long> seatIds);
}
