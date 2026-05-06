package com.concertticketing.domain.seat.repository;

import com.concertticketing.domain.seat.entity.Seat;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaSeatRepositoryAdapter implements SeatRepository {

    private final SpringDataSeatRepository delegate;

    public JpaSeatRepositoryAdapter(SpringDataSeatRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Seat save(Seat seat) {
        return delegate.save(seat);
    }

    @Override
    public List<Seat> findByScheduleId(Long scheduleId) {
        return delegate.findByScheduleIdOrderByIdAsc(scheduleId);
    }

    @Override
    public Optional<Seat> findById(Long seatId) {
        return delegate.findById(seatId);
    }

    @Override
    public List<Seat> findAllByIds(List<Long> seatIds) {
        return delegate.findByIdIn(seatIds);
    }
}
