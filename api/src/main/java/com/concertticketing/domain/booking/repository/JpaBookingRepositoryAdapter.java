package com.concertticketing.domain.booking.repository;

import com.concertticketing.domain.booking.entity.Booking;
import com.concertticketing.domain.booking.entity.BookingStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaBookingRepositoryAdapter implements BookingRepository {

    private final SpringDataBookingRepository delegate;

    public JpaBookingRepositoryAdapter(SpringDataBookingRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Booking save(Booking booking) {
        return delegate.save(booking);
    }

    @Override
    public Optional<Booking> findById(Long bookingId) {
        Optional<Booking> booking = delegate.findById(bookingId);
        booking.ifPresent(b -> b.getSeatIds().size()); // EAGER fetch 보장 (방어적)
        return booking;
    }

    @Override
    public List<Booking> findByUserId(Long userId, int page, int size) {
        return delegate.findByUserId(
                userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();
    }

    @Override
    public long countByUserId(Long userId) {
        return delegate.countByUserId(userId);
    }

    @Override
    public int countSeatsByUserIdAndScheduleId(Long userId, Long scheduleId) {
        return delegate.sumSeatsByUserAndScheduleExceptStatus(userId, scheduleId, BookingStatus.CANCELLED);
    }
}
