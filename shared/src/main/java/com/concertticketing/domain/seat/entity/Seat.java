package com.concertticketing.domain.seat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "seats")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long scheduleId;

    @Column(nullable = false)
    private String seatNumber;

    @Column(nullable = false)
    private String grade;

    @Column(nullable = false)
    private int price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status;

    protected Seat() {
    }

    public Seat(Long scheduleId, String seatNumber, String grade, int price) {
        this.scheduleId = scheduleId;
        this.seatNumber = seatNumber;
        this.grade = grade;
        this.price = price;
        this.status = SeatStatus.AVAILABLE;
    }

    /**
     * 좌석 판매 처리
     */
    public void markAsSold() {
        if (this.status == SeatStatus.SOLD) {
            throw new IllegalStateException("이미 판매된 좌석입니다.");
        }
        this.status = SeatStatus.SOLD;
    }

    /**
     * 좌석 판매 취소 (예매 취소 시)
     */
    public void markAsAvailable() {
        this.status = SeatStatus.AVAILABLE;
    }

    public Long getId() {
        return id;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public String getGrade() {
        return grade;
    }

    public int getPrice() {
        return price;
    }

    public SeatStatus getStatus() {
        return status;
    }
}
