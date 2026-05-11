package com.concertticketing.domain.booking.repository;

import com.concertticketing.domain.booking.entity.Booking;
import com.concertticketing.domain.booking.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataBookingRepository extends JpaRepository<Booking, Long> {

    long countByUserId(Long userId);

    Page<Booking> findByUserId(Long userId, Pageable pageable);

    @Query("""
            select coalesce(sum(size(b.seatIds)), 0)
            from Booking b
            where b.userId = :userId
              and b.scheduleId = :scheduleId
              and b.status <> :excludedStatus
            """)
    int sumSeatsByUserAndScheduleExceptStatus(@Param("userId") Long userId,
                                              @Param("scheduleId") Long scheduleId,
                                              @Param("excludedStatus") BookingStatus excludedStatus);
}
