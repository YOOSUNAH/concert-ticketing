package com.concertticketing.domain.schedule.repository;

import com.concertticketing.domain.schedule.entity.ScheduleRef;

import java.util.List;
import java.util.Optional;

public interface ScheduleRefRepository {

    ScheduleRef save(ScheduleRef scheduleRef);

    Optional<ScheduleRef> findById(Long id);

    List<ScheduleRef> findByConcertId(Long concertId);
}
