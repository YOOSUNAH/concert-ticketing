package com.concertticketing.domain.concert.service;

import com.concertticketing.domain.concert.dto.ConcertWithSchedules;
import com.concertticketing.domain.concert.entity.Concert;
import com.concertticketing.domain.concert.repository.ConcertRepository;
import com.concertticketing.domain.schedule.entity.Schedule;
import com.concertticketing.domain.schedule.repository.ScheduleRepository;

import java.util.List;

public class ConcertService {

    private final ConcertRepository concertRepository;
    private final ScheduleRepository scheduleRepository;

    public ConcertService(ConcertRepository concertRepository,
                          ScheduleRepository scheduleRepository) {
        this.concertRepository = concertRepository;
        this.scheduleRepository = scheduleRepository;
    }

    /**
     * 콘서트 목록 조회 (페이징)
     */
    public List<Concert> getConcerts(int page, int size) {
        return concertRepository.findAll(page, size);
    }

    /**
     * 전체 콘서트 수 (페이징 정보용)
     */
    public long getTotalCount() {
        return concertRepository.count();
    }

    /**
     * 콘서트 상세 조회 (스케줄 포함)
     */
    public ConcertWithSchedules getConcertDetail(Long concertId) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 콘서트입니다."));

        List<Schedule> schedules = scheduleRepository.findByConcertId(concertId);

        return new ConcertWithSchedules(concert, schedules);
    }
}
