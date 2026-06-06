package com.concertticketing.admin.api.service;

import com.concertticketing.admin.api.dto.ConcertCreateRequest;
import com.concertticketing.admin.api.dto.ConcertResponse;
import com.concertticketing.admin.api.dto.ConcertUpdateRequest;
import com.concertticketing.admin.api.dto.ScheduleCreateRequest;
import com.concertticketing.admin.api.dto.SeatGroupRequest;
import com.concertticketing.domain.concert.entity.Concert;
import com.concertticketing.domain.concert.entity.ConcertRef;
import com.concertticketing.domain.concert.entity.ConcertStatus;
import com.concertticketing.domain.concert.repository.ConcertRefRepository;
import com.concertticketing.domain.concert.repository.ConcertRepository;
import com.concertticketing.domain.schedule.entity.ScheduleRef;
import com.concertticketing.domain.schedule.repository.ScheduleRefRepository;
import com.concertticketing.domain.seat.entity.Seat;
import com.concertticketing.domain.seat.repository.SeatRepository;
import com.concertticketing.domain.soldout.SoldOutService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminConcertServiceTest {

    @Mock ConcertRefRepository concertRefRepository;
    @Mock ScheduleRefRepository scheduleRefRepository;
    @Mock SeatRepository seatRepository;
    @Mock ConcertRepository concertRepository;
    @Mock SoldOutService soldOutService;

    @InjectMocks AdminConcertService sut;

    // ===== createConcert: dual-write 검증 =====

    @Test
    void 공연_생성_시_PostgreSQL에_ConcertRef가_저장된다() {
        // given
        ConcertCreateRequest request = createRequest();
        stubConcertRefSave(1L);
        stubScheduleRefSave(10L);

        // when
        sut.createConcert(request);

        // then
        ArgumentCaptor<ConcertRef> captor = ArgumentCaptor.forClass(ConcertRef.class);
        verify(concertRefRepository).save(captor.capture());
        ConcertRef saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("테스트 콘서트");
        assertThat(saved.getVenue()).isEqualTo("올림픽홀");
        assertThat(saved.getMaxTicketsPerPerson()).isEqualTo(4);
    }

    @Test
    void 공연_생성_시_PostgreSQL에_ScheduleRef가_저장된다() {
        // given
        ConcertCreateRequest request = createRequest();
        stubConcertRefSave(1L);
        stubScheduleRefSave(10L);

        // when
        sut.createConcert(request);

        // then
        ArgumentCaptor<ScheduleRef> captor = ArgumentCaptor.forClass(ScheduleRef.class);
        verify(scheduleRefRepository).save(captor.capture());
        ScheduleRef saved = captor.getValue();
        assertThat(saved.getConcertId()).isEqualTo(1L);
        assertThat(saved.getTotalSeats()).isEqualTo(5); // A구역 3 + B구역 2
    }

    @Test
    void 공연_생성_시_PostgreSQL에_Seat_행이_생성된다() {
        // given
        ConcertCreateRequest request = createRequest();
        stubConcertRefSave(1L);
        stubScheduleRefSave(10L);
        when(seatRepository.save(any(Seat.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        sut.createConcert(request);

        // then: A구역 3석 + B구역 2석 = 5번 save
        verify(seatRepository, times(5)).save(any(Seat.class));
    }

    @Test
    void 공연_생성_시_MongoDB에_Concert_문서가_저장된다() {
        // given
        ConcertCreateRequest request = createRequest();
        stubConcertRefSave(1L);
        stubScheduleRefSave(10L);

        // when
        sut.createConcert(request);

        // then
        ArgumentCaptor<Concert> captor = ArgumentCaptor.forClass(Concert.class);
        verify(concertRepository).save(captor.capture());
        Concert saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getTitle()).isEqualTo("테스트 콘서트");
        assertThat(saved.getSchedules()).hasSize(1);
        assertThat(saved.getSchedules().get(0).getId()).isEqualTo(10L);
    }

    @Test
    void 공연_생성_시_Redis_잔여좌석_카운터가_초기화된다() {
        // given
        ConcertCreateRequest request = createRequest();
        stubConcertRefSave(1L);
        stubScheduleRefSave(10L);

        // when
        sut.createConcert(request);

        // then
        verify(soldOutService).initRemainingSeats(eq(10L), eq(5), any(LocalDate.class));
    }

    @Test
    void 공연_생성_응답에_올바른_정보가_포함된다() {
        // given
        ConcertCreateRequest request = createRequest();
        stubConcertRefSave(1L);
        stubScheduleRefSave(10L);

        // when
        ConcertResponse response = sut.createConcert(request);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("테스트 콘서트");
        assertThat(response.schedules()).hasSize(1);
        assertThat(response.schedules().get(0).totalSeats()).isEqualTo(5);
    }

    // ===== updateConcert: dual-write 검증 =====

    @Test
    void 공연_수정_시_PostgreSQL과_MongoDB_모두_업데이트된다() {
        // given
        ConcertRef existingRef = new ConcertRef("원래 제목", "원래 장소", 4, ConcertStatus.OPEN);
        when(concertRefRepository.findById(1L)).thenReturn(Optional.of(existingRef));

        Concert existingConcert = new Concert(1L, "원래 제목", "아티스트", "설명",
                "원래 장소", null, null,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2), 4, ConcertStatus.OPEN);
        when(concertRepository.findById(1L)).thenReturn(Optional.of(existingConcert));

        ConcertUpdateRequest updateReq = new ConcertUpdateRequest(
                "수정된 제목", "수정된 아티스트", "수정된 설명",
                "수정된 장소", null, null,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2), 2
        );

        // when
        ConcertResponse response = sut.updateConcert(1L, updateReq);

        // then: PostgreSQL 저장
        verify(concertRefRepository).save(existingRef);
        assertThat(existingRef.getTitle()).isEqualTo("수정된 제목");

        // then: MongoDB 저장
        ArgumentCaptor<Concert> captor = ArgumentCaptor.forClass(Concert.class);
        verify(concertRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("수정된 제목");

        // then: 응답
        assertThat(response.title()).isEqualTo("수정된 제목");
    }

    @Test
    void 존재하지_않는_공연_수정_시_예외발생() {
        when(concertRefRepository.findById(999L)).thenReturn(Optional.empty());

        ConcertUpdateRequest updateReq = new ConcertUpdateRequest(
                "제목", "아티스트", "설명", "장소", null, null,
                LocalDate.now(), LocalDate.now(), 2
        );

        assertThatThrownBy(() -> sut.updateConcert(999L, updateReq))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ===== addSchedule: dual-write 검증 =====

    @Test
    void 스케줄_추가_시_PostgreSQL과_MongoDB_Redis_모두_반영된다() {
        // given
        ConcertRef concertRef = new ConcertRef("콘서트", "장소", 4, ConcertStatus.OPEN);
        when(concertRefRepository.findById(1L)).thenReturn(Optional.of(concertRef));

        Concert existingConcert = new Concert(1L, "콘서트", "아티스트", "설명",
                "장소", null, null,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2), 4, ConcertStatus.OPEN);
        when(concertRepository.findById(1L)).thenReturn(Optional.of(existingConcert));
        stubScheduleRefSave(20L);

        ScheduleCreateRequest scheduleReq = new ScheduleCreateRequest(
                LocalDate.of(2026, 7, 3), LocalTime.of(19, 0),
                List.of(new SeatGroupRequest("VIP", "V", 10, 150000))
        );

        // when
        sut.addSchedule(1L, scheduleReq);

        // then
        verify(scheduleRefRepository).save(any(ScheduleRef.class)); // PostgreSQL
        verify(seatRepository, times(10)).save(any(Seat.class));    // PostgreSQL: 10석
        verify(concertRepository).save(any(Concert.class));          // MongoDB
        verify(soldOutService).initRemainingSeats(eq(20L), eq(10), any(LocalDate.class)); // Redis
    }

    // ===== helpers =====

    private ConcertCreateRequest createRequest() {
        return new ConcertCreateRequest(
                "테스트 콘서트", "테스트 아티스트", "테스트 설명",
                "올림픽홀", "poster.jpg", "thumb.jpg",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2),
                4,
                List.of(new ScheduleCreateRequest(
                        LocalDate.of(2026, 7, 1), LocalTime.of(19, 0),
                        List.of(
                                new SeatGroupRequest("A", "A", 3, 100000),
                                new SeatGroupRequest("B", "B", 2, 80000)
                        )
                ))
        );
    }

    private void stubConcertRefSave(Long id) {
        when(concertRefRepository.save(any(ConcertRef.class))).thenAnswer(invocation -> {
            ConcertRef ref = invocation.getArgument(0);
            // ConcertRef의 id는 JPA가 설정하므로 reflection으로 주입
            var field = ConcertRef.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(ref, id);
            return ref;
        });
    }

    private void stubScheduleRefSave(Long id) {
        when(scheduleRefRepository.save(any(ScheduleRef.class))).thenAnswer(invocation -> {
            ScheduleRef ref = invocation.getArgument(0);
            var field = ScheduleRef.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(ref, id);
            return ref;
        });
    }
}
