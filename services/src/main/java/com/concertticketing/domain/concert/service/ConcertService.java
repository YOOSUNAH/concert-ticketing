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

    /** 콘서트 목록 조회 (페이징) */
    public List<Concert> getConcerts(int page, int size) {
        return concertRepository.findAll(page, size);
    }

    /** 전체 콘서트 수 (페이징 정보용) */
    public long getTotalCount() {
        return concertRepository.count();
    }

    /** 콘서트 상세 조회 (스케줄 포함) — 임베디드라 단일 쿼리 */
    public ConcertWithSchedules getConcertDetail(Long concertId) {
        Concert concert = getConcert(concertId);
        return new ConcertWithSchedules(concert, concert.getSchedules());
    }

    /** scheduleId로 소속 콘서트 조회 (임베디드 schedules.id 매치) */
    public Concert getConcertByScheduleId(Long scheduleId) {
        Concert concert = concertRepository.findFirstBySchedulesIdEquals(scheduleId);
        if (concert == null) {
            throw new IllegalArgumentException("존재하지 않는 스케줄입니다.");
        }
        return concert;
    }

    public Schedule getSchedule(Long scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스케줄입니다."));
    }

    public Concert getConcert(Long concertId) {
        return concertRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 콘서트입니다."));
    }

    public List<Schedule> getSchedulesByIds(List<Long> scheduleIds) {
        return scheduleRepository.findAllByIds(scheduleIds);
    }

    public List<Concert> getConcertsByIds(List<Long> concertIds) {
        return concertRepository.findAllByIds(concertIds);
    }
}
