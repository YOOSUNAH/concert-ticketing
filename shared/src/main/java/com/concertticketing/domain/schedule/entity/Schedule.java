package com.concertticketing.domain.schedule.entity;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Concert 문서에 임베디드되는 스케줄.
 * MongoDB 컬렉션이 아니며, Concert.schedules[]에 포함된다.
 */
public class Schedule {

    private Long id;
    private Long concertId;
    private LocalDate date;
    private LocalTime time;
    private int totalSeats;
    private int remainingSeats;

    protected Schedule() {
    }

    public Schedule(Long id, Long concertId, LocalDate date, LocalTime time,
                    int totalSeats, int remainingSeats) {
        this.id = id;
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
