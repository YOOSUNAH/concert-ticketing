package com.concertticketing.admin.api.repository;

import com.concertticketing.domain.schedule.entity.ScheduleRef;
import com.concertticketing.domain.schedule.repository.ScheduleRefRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaScheduleRefRepositoryAdapter implements ScheduleRefRepository {

    private final SpringDataScheduleRefRepository delegate;

    public JpaScheduleRefRepositoryAdapter(SpringDataScheduleRefRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public ScheduleRef save(ScheduleRef scheduleRef) {
        return delegate.save(scheduleRef);
    }

    @Override
    public Optional<ScheduleRef> findById(Long id) {
        return delegate.findById(id);
    }

    @Override
    public List<ScheduleRef> findByConcertId(Long concertId) {
        return delegate.findByConcertId(concertId);
    }
}
