package com.concertticketing.domain.booking.entity;

import java.time.LocalDateTime;

public class Booking {

    private Long id;
    private Long userId;
    private Long scheduleId;
    private String bookingNumber;
    private Long seatId;
    private int amount;
    private Long paymentId;
    private BookingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime cancelledAt;

    protected Booking() {
    }

    public Booking(Long userId, Long scheduleId, String bookingNumber,
                   Long seatId, int amount) {
        this.userId = userId;
        this.scheduleId = scheduleId;
        this.bookingNumber = bookingNumber;
        this.seatId = seatId;
        this.amount = amount;
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

    /**
     * 결제 묶음 식별자 연결
     * - 같은 클릭으로 생성된 N개 Booking을 하나의 Payment로 묶기 위함
     */
    public void linkToPayment(Long paymentId) {
        this.paymentId = paymentId;
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

    public Long getSeatId() {
        return seatId;
    }

    public int getAmount() {
        return amount;
    }

    public Long getPaymentId() {
        return paymentId;
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
