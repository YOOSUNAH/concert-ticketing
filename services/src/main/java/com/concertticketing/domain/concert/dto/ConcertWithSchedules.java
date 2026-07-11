package com.concertticketing.domain.concert.dto;

import com.concertticketing.domain.concert.entity.Concert;
import com.concertticketing.domain.schedule.entity.Schedule;

import java.util.List;

public class ConcertWithSchedules {

    private final Concert concert;
    private final List<Schedule> schedules;

    public ConcertWithSchedules(Concert concert, List<Schedule> schedules) {
        this.concert = concert;
        this.schedules = schedules;
    }

    public Concert getConcert() {
        return concert;
    }

    public List<Schedule> getSchedules() {
        return schedules;
    }
}
