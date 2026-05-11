package com.concertticketing.domain.schedule.repository;

import com.concertticketing.domain.schedule.entity.Schedule;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaScheduleRepositoryAdapter implements ScheduleRepository {

    private final SpringDataScheduleRepository delegate;

    public JpaScheduleRepositoryAdapter(SpringDataScheduleRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Schedule save(Schedule schedule) {
        return delegate.save(schedule);
    }

    @Override
    public List<Schedule> findByConcertId(Long concertId) {
        return delegate.findByConcertIdOrderByIdAsc(concertId);
    }

    @Override
    public Optional<Schedule> findById(Long scheduleId) {
        return delegate.findById(scheduleId);
    }

    @Override
    public List<Schedule> findAllByIds(List<Long> scheduleIds) {
        return delegate.findAllById(scheduleIds);
    }
}
