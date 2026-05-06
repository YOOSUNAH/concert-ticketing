package com.concertticketing.domain.schedule.repository;

import com.concertticketing.domain.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByConcertIdOrderByIdAsc(Long concertId);
}
