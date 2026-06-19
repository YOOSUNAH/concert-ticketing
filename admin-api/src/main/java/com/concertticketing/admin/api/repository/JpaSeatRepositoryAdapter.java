package com.concertticketing.admin.api.repository;

import com.concertticketing.domain.seat.entity.Seat;
import com.concertticketing.domain.seat.repository.SeatRepository;
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
        return delegate.findAll(); // admin에서는 미사용
    }

    @Override
    public Optional<Seat> findById(Long seatId) {
        return delegate.findById(seatId);
    }

    @Override
    public List<Seat> findAllByIds(List<Long> seatIds) {
        return delegate.findAllById(seatIds);
    }

    @Override
    public int markAsSoldWhereAvailable(List<Long> seatIds) {
        throw new UnsupportedOperationException("admin-api에서는 좌석 판매를 처리하지 않습니다.");
    }

    @Override
    public int markAsAvailableWhereSold(List<Long> seatIds) {
        throw new UnsupportedOperationException("admin-api에서는 좌석 판매를 처리하지 않습니다.");
    }
}
