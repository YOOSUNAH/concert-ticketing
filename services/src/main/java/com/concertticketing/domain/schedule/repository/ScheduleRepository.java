package com.concertticketing.domain.schedule.repository;

import com.concertticketing.domain.schedule.entity.Schedule;

import java.util.List;
import java.util.Optional;

public interface ScheduleRepository {

    List<Schedule> findByConcertId(Long concertId);

    Optional<Schedule> findById(Long scheduleId);
}
