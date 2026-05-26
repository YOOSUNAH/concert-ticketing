package com.concertticketing.domain.concert.service;

import com.concertticketing.domain.concert.entity.ConcertRef;
import com.concertticketing.domain.concert.repository.ConcertRefRepository;
import com.concertticketing.domain.schedule.entity.ScheduleRef;
import com.concertticketing.domain.schedule.repository.ScheduleRefRepository;

/**
 * PostgreSQL 기반 콘서트/스케줄 조회 서비스.
 * Main API의 BookingService가 예매 시 사용한다.
 */
public class ConcertRefService {

    private final ConcertRefRepository concertRefRepository;
    private final ScheduleRefRepository scheduleRefRepository;

    public ConcertRefService(ConcertRefRepository concertRefRepository,
                             ScheduleRefRepository scheduleRefRepository) {
        this.concertRefRepository = concertRefRepository;
        this.scheduleRefRepository = scheduleRefRepository;
    }

    public ConcertRef getConcertByScheduleId(Long scheduleId) {
        ScheduleRef schedule = getSchedule(scheduleId);
        return concertRefRepository.findById(schedule.getConcertId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 콘서트입니다."));
    }

    public ScheduleRef getSchedule(Long scheduleId) {
        return scheduleRefRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스케줄입니다."));
    }
}
