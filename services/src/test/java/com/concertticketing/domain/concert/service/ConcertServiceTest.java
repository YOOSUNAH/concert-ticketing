package com.concertticketing.domain.concert.service;

import com.concertticketing.domain.concert.entity.Concert;
import com.concertticketing.domain.concert.entity.ConcertStatus;
import com.concertticketing.domain.concert.repository.ConcertRepository;
import com.concertticketing.domain.schedule.repository.ScheduleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

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
    @DisplayName("scheduleId로 콘서트 조회 성공 — 임베디드 매치")
    void getConcertByScheduleId_success() {
        Long scheduleId = 7L;
        Concert concert = new Concert(1L, "10cm 콘서트", "10cm", "설명", "올림픽공원",
                null, null, LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 2),
                2, ConcertStatus.OPEN);
        when(concertRepository.findFirstBySchedulesIdEquals(scheduleId)).thenReturn(concert);

        Concert result = concertService.getConcertByScheduleId(scheduleId);

        assertEquals("10cm 콘서트", result.getTitle());
    }

    @Test
    @DisplayName("scheduleId로 콘서트 조회 실패 — 매치되는 Concert 없음")
    void getConcertByScheduleId_notFound_throwsException() {
        when(concertRepository.findFirstBySchedulesIdEquals(1L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> concertService.getConcertByScheduleId(1L));
    }
}
