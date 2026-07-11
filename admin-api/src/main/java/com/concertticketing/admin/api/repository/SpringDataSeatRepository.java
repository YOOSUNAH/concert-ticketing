package com.concertticketing.admin.api.repository;

import com.concertticketing.domain.seat.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataSeatRepository extends JpaRepository<Seat, Long> {
}
