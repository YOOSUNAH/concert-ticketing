package com.concertticketing.domain.concert.service;

import com.concertticketing.domain.concert.entity.ConcertRef;
import com.concertticketing.domain.concert.entity.ConcertStatus;
import com.concertticketing.domain.concert.repository.ConcertRefRepository;
import com.concertticketing.domain.schedule.entity.ScheduleRef;
import com.concertticketing.domain.schedule.repository.ScheduleRefRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConcertRefServiceTest {

    @Mock ConcertRefRepository concertRefRepository;
    @Mock ScheduleRefRepository scheduleRefRepository;

    @InjectMocks ConcertRefService sut;

    @Test
    void 스케줄ID로_ConcertRef를_정상_조회한다() {
        // given
        ScheduleRef scheduleRef = createScheduleRef(10L, 1L);
        when(scheduleRefRepository.findById(10L)).thenReturn(Optional.of(scheduleRef));

        ConcertRef concertRef = new ConcertRef("콘서트", "올림픽홀", 4, ConcertStatus.OPEN);
        when(concertRefRepository.findById(1L)).thenReturn(Optional.of(concertRef));

        // when
        ConcertRef result = sut.getConcertByScheduleId(10L);

        // then
        assertThat(result.getTitle()).isEqualTo("콘서트");
        assertThat(result.getVenue()).isEqualTo("올림픽홀");
    }

    @Test
    void 스케줄ID로_조회_시_스케줄이_없으면_예외() {
        when(scheduleRefRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.getConcertByScheduleId(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 스케줄");
    }

    @Test
    void 스케줄ID로_조회_시_콘서트가_없으면_예외() {
        ScheduleRef scheduleRef = createScheduleRef(10L, 999L);
        when(scheduleRefRepository.findById(10L)).thenReturn(Optional.of(scheduleRef));
        when(concertRefRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.getConcertByScheduleId(10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 콘서트");
    }

    @Test
    void getSchedule_정상_조회() {
        ScheduleRef scheduleRef = createScheduleRef(10L, 1L);
        when(scheduleRefRepository.findById(10L)).thenReturn(Optional.of(scheduleRef));

        ScheduleRef result = sut.getSchedule(10L);

        assertThat(result.getConcertId()).isEqualTo(1L);
        assertThat(result.getTotalSeats()).isEqualTo(100);
    }

    @Test
    void getSchedule_존재하지_않으면_예외() {
        when(scheduleRefRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.getSchedule(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 스케줄");
    }

    private ScheduleRef createScheduleRef(Long id, Long concertId) {
        ScheduleRef ref = new ScheduleRef(concertId, LocalDate.of(2026, 7, 1), LocalTime.of(19, 0), 100);
        try {
            var field = ScheduleRef.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(ref, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ref;
    }
}
