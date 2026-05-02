package com.concertticketing.domain.schedule.entity;

import java.time.LocalDate;
import java.time.LocalTime;

public class Schedule {

    private Long id;
    private Long concertId;
    private LocalDate date;
    private LocalTime time;
    private int totalSeats;
    private int remainingSeats;

    protected Schedule() {
    }

    public Schedule(Long concertId, LocalDate date, LocalTime time,
                    int totalSeats, int remainingSeats) {
        this.concertId = concertId;
        this.date = date;
        this.time = time;
        this.totalSeats = totalSeats;
        this.remainingSeats = remainingSeats;
    }

    public Long getId() {
        return id;
    }

    public Long getConcertId() {
        return concertId;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getTime() {
        return time;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public int getRemainingSeats() {
        return remainingSeats;
    }
}
