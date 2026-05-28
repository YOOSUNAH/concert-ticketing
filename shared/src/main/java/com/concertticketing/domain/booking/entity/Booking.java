package com.concertticketing.domain.booking.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long scheduleId;

    @Column(nullable = false, unique = true)
    private String bookingNumber;

    // N+1 방어: application.yml의 hibernate.default_batch_fetch_size로 IN 묶음 조회
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "booking_seats",
            joinColumns = @JoinColumn(name = "booking_id")
    )
    @Column(name = "seat_id", nullable = false)
    private List<Long> seatIds;

    @Column(nullable = false)
    private int totalAmount;

    @Column(nullable = false)
    private String concertTitle;

    @Column(nullable = false)
    private LocalDate scheduleDate;

    @Column(nullable = false)
    private LocalTime scheduleTime;

    @Column(nullable = false)
    private String venueName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime cancelledAt;

    protected Booking() {
    }

    public Booking(Long userId, Long scheduleId, String bookingNumber,
                   List<Long> seatIds, int totalAmount,
                   String concertTitle, LocalDate scheduleDate,
                   LocalTime scheduleTime, String venueName) {
        this.userId = userId;
        this.scheduleId = scheduleId;
        this.bookingNumber = bookingNumber;
        this.seatIds = seatIds;
        this.totalAmount = totalAmount;
        this.concertTitle = concertTitle;
        this.scheduleDate = scheduleDate;
        this.scheduleTime = scheduleTime;
        this.venueName = venueName;
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

    public String getConcertTitle() {
        return concertTitle;
    }

    public LocalDate getScheduleDate() {
        return scheduleDate;
    }

    public LocalTime getScheduleTime() {
        return scheduleTime;
    }

    public String getVenueName() {
        return venueName;
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
