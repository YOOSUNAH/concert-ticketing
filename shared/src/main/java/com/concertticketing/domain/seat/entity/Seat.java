package com.concertticketing.domain.seat.entity;

public class Seat {

    private Long id;
    private Long scheduleId;
    private String seatNumber;
    private String grade;
    private int price;
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
