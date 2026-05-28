package com.concertticketing.domain.schedule.repository;

import com.concertticketing.domain.concert.entity.Concert;
import com.concertticketing.domain.concert.repository.ConcertRepository;
import com.concertticketing.domain.schedule.entity.Schedule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Schedule은 Concert 문서에 임베디드되어 별도 컬렉션이 아니다.
 * Concert를 통해 Schedule을 조회/추출하는 래퍼.
 * MongoRepository에 의존하므로 mongo를 쓰는 모듈에서만 명시적으로 등록.
 */
public class ScheduleRepository {

    private final ConcertRepository concertRepository;

    public ScheduleRepository(ConcertRepository concertRepository) {
        this.concertRepository = concertRepository;
    }

    public List<Schedule> findByConcertId(Long concertId) {
        return concertRepository.findById(concertId)
                .map(Concert::getSchedules)
                .orElse(List.of());
    }

    public Optional<Schedule> findById(Long scheduleId) {
        Concert concert = concertRepository.findFirstBySchedulesIdEquals(scheduleId);
        if (concert == null) {
            return Optional.empty();
        }
        return concert.getSchedules().stream()
                .filter(s -> s.getId().equals(scheduleId))
                .findFirst();
    }

    public List<Schedule> findAllByIds(List<Long> scheduleIds) {
        Set<Long> ids = new HashSet<>(scheduleIds);
        List<Schedule> result = new ArrayList<>();
        for (Concert concert : concertRepository.findAll()) {
            for (Schedule schedule : concert.getSchedules()) {
                if (ids.contains(schedule.getId())) {
                    result.add(schedule);
                }
            }
        }
        return result;
    }
}
