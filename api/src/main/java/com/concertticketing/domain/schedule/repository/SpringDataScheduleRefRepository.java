package com.concertticketing.domain.schedule.repository;

import com.concertticketing.domain.schedule.entity.ScheduleRef;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataScheduleRefRepository extends JpaRepository<ScheduleRef, Long> {

    List<ScheduleRef> findByConcertId(Long concertId);
}
