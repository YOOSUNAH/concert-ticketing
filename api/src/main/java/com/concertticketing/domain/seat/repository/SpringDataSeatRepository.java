package com.concertticketing.domain.seat.repository;

import com.concertticketing.domain.seat.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataSeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByScheduleIdOrderByIdAsc(Long scheduleId);

    List<Seat> findByIdIn(List<Long> seatIds);

    @Modifying
    @Query("UPDATE Seat s SET s.status = 'SOLD' WHERE s.id IN :seatIds AND s.status = 'AVAILABLE'")
    int markAsSoldWhereAvailable(@Param("seatIds") List<Long> seatIds);

    @Modifying
    @Query("UPDATE Seat s SET s.status = 'AVAILABLE' WHERE s.id IN :seatIds AND s.status = 'SOLD'")
    int markAsAvailableWhereSold(@Param("seatIds") List<Long> seatIds);
}
