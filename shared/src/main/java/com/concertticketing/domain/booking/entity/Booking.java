package com.concertticketing.domain.booking.entity;

import java.time.LocalDateTime;
import java.util.List;

public class Booking {

    private Long id;
    private Long userId;
    private Long scheduleId;
    private String bookingNumber;
    private List<Long> seatIds;
    private int totalAmount;
    private BookingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime cancelledAt;

    protected Booking() {
    }

    public Booking(Long userId, Long scheduleId, String bookingNumber,
                   List<Long> seatIds, int totalAmount) {
        this.userId = userId;
        this.scheduleId = scheduleId;
        this.bookingNumber = bookingNumber;
        this.seatIds = seatIds;
        this.totalAmount = totalAmount;
        this.status = BookingStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 결제 완료 처리
     */
    public void markAsPaid() {
        if (this.status != BookingStatus.PENDING) {
            throw new IllegalStateException("결제 대기 상태에서만 결제할 수 있습니다.");
        }
        this.status = BookingStatus.PAID;
    }

    /**
     * 예매 취소 처리
     */
    public void cancel() {
        if (this.status == BookingStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 예매입니다.");
        }
        this.status = BookingStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public String getBookingNumber() {
        return bookingNumber;
    }

    public List<Long> getSeatIds() {
        return seatIds;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }
}
