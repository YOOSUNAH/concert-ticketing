package com.concertticketing.domain.concert.service;

import com.concertticketing.domain.concert.entity.Concert;
import com.concertticketing.domain.concert.entity.ConcertStatus;
import com.concertticketing.domain.concert.repository.ConcertRepository;
import com.concertticketing.domain.schedule.entity.Schedule;
import com.concertticketing.domain.schedule.repository.ScheduleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConcertServiceTest {

    @Mock
    ConcertRepository concertRepository;

    @Mock
    ScheduleRepository scheduleRepository;

    @InjectMocks
    ConcertService concertService;

    @Test
    @DisplayName("scheduleId로 콘서트 조회 성공")
    void getConcertByScheduleId_success() {
        Long scheduleId = 1L;
        Long concertId = 10L;
        Schedule schedule = new Schedule(concertId, LocalDate.of(2025, 8, 1), LocalTime.of(19, 0), 500, 120);
        Concert concert = new Concert("10cm 콘서트", "10cm", "설명", "올림픽공원",
                null, null, LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 2),
                2, ConcertStatus.OPEN);
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(concertRepository.findById(concertId)).thenReturn(Optional.of(concert));

        Concert result = concertService.getConcertByScheduleId(scheduleId);

        assertEquals("10cm 콘서트", result.getTitle());
    }

    @Test
    @DisplayName("scheduleId로 콘서트 조회 실패 - 존재하지 않는 스케줄")
    void getConcertByScheduleId_scheduleNotFound_throwsException() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> concertService.getConcertByScheduleId(1L));
    }

    @Test
    @DisplayName("scheduleId로 콘서트 조회 실패 - 스케줄은 있지만 concert 없음")
    void getConcertByScheduleId_concertNotFound_throwsException() {
        Long scheduleId = 1L;
        Long concertId = 10L;
        Schedule schedule = new Schedule(concertId, LocalDate.of(2025, 8, 1), LocalTime.of(19, 0), 500, 120);
        when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(concertRepository.findById(concertId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> concertService.getConcertByScheduleId(scheduleId));
    }
}
