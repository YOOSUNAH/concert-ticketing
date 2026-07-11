package com.concertticketing.domain.schedule.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * PostgreSQL용 스케줄 참조 엔티티.
 * Main API가 예매 시 필요한 최소 정보만 보유한다.
 * MongoDB의 Schedule(Concert에 임베디드)과 동일한 ID를 공유한다.
 */
@Entity
@Table(name = "schedules")
public class ScheduleRef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long concertId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime time;

    @Column(nullable = false)
    private int totalSeats;

    protected ScheduleRef() {
    }

    public ScheduleRef(Long concertId, LocalDate date, LocalTime time, int totalSeats) {
        this.concertId = concertId;
        this.date = date;
        this.time = time;
        this.totalSeats = totalSeats;
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
}
