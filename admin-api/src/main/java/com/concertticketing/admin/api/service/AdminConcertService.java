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
import com.concertticketing.domain.schedule.entity.Schedule;
import com.concertticketing.domain.schedule.entity.ScheduleRef;
import com.concertticketing.domain.schedule.repository.ScheduleRefRepository;
import com.concertticketing.domain.seat.entity.Seat;
import com.concertticketing.domain.seat.repository.SeatRepository;
import com.concertticketing.domain.soldout.SoldOutService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class AdminConcertService {

    private final ConcertRefRepository concertRefRepository;
    private final ScheduleRefRepository scheduleRefRepository;
    private final SeatRepository seatRepository;
    private final ConcertRepository concertRepository;
    private final SoldOutService soldOutService;

    public AdminConcertService(ConcertRefRepository concertRefRepository,
                               ScheduleRefRepository scheduleRefRepository,
                               SeatRepository seatRepository,
                               ConcertRepository concertRepository,
                               SoldOutService soldOutService) {
        this.concertRefRepository = concertRefRepository;
        this.scheduleRefRepository = scheduleRefRepository;
        this.seatRepository = seatRepository;
        this.concertRepository = concertRepository;
        this.soldOutService = soldOutService;
    }

    /**
     * 공연 생성 (dual-write)
     * 1. PostgreSQL: ConcertRef 저장 → ID 확보
     * 2. PostgreSQL: ScheduleRef 저장 → ID 확보
     * 3. PostgreSQL: Seat 행 생성
     * 4. MongoDB: Concert 문서 저장 (PostgreSQL에서 생성된 ID 사용)
     * 5. Redis: 잔여 좌석 카운터 초기화
     *
     * MongoDB 쓰기 실패 시 RuntimeException → @Transactional이 PostgreSQL 롤백
     */
    @Transactional
    public ConcertResponse createConcert(ConcertCreateRequest request) {
        // 1. PostgreSQL: ConcertRef 저장
        ConcertRef concertRef = new ConcertRef(
                request.title(), request.venue(),
                request.maxTicketsPerPerson(), ConcertStatus.OPEN
        );
        concertRef = concertRefRepository.save(concertRef);
        Long concertId = concertRef.getId();

        // 2-3. 각 스케줄별로 ScheduleRef + Seat 생성
        Concert concert = new Concert(
                concertId, request.title(), request.artist(), request.description(),
                request.venue(), request.posterUrl(), request.thumbnailUrl(),
                request.startDate(), request.endDate(),
                request.maxTicketsPerPerson(), ConcertStatus.OPEN
        );

        for (ScheduleCreateRequest scheduleReq : request.schedules()) {
            // PostgreSQL: ScheduleRef
            ScheduleRef scheduleRef = new ScheduleRef(
                    concertId, scheduleReq.date(), scheduleReq.time(),
                    countTotalSeats(scheduleReq.seatGroups())
            );
            scheduleRef = scheduleRefRepository.save(scheduleRef);
            Long scheduleId = scheduleRef.getId();

            // PostgreSQL: Seat 행 생성
            createSeats(scheduleId, scheduleReq.seatGroups());

            // MongoDB용 Schedule (동일 ID)
            int totalSeats = countTotalSeats(scheduleReq.seatGroups());
            concert.addSchedule(new Schedule(
                    scheduleId, concertId,
                    scheduleReq.date(), scheduleReq.time(),
                    totalSeats, totalSeats
            ));

            // 5. Redis: 잔여 좌석 카운터 초기화
            soldOutService.initRemainingSeats(scheduleId, totalSeats);
        }

        // 4. MongoDB: Concert 문서 저장
        concertRepository.save(concert);

        return ConcertResponse.from(concert);
    }

    /**
     * 공연 정보 수정 (dual-write)
     */
    @Transactional
    public ConcertResponse updateConcert(Long concertId, ConcertUpdateRequest request) {
        // PostgreSQL 수정
        ConcertRef concertRef = concertRefRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 콘서트입니다."));
        concertRef.updateInfo(request.title(), request.venue(), request.maxTicketsPerPerson());
        concertRefRepository.save(concertRef);

        // MongoDB 수정: 기존 문서를 교체
        Concert existing = concertRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("MongoDB에 콘서트가 존재하지 않습니다."));
        Concert updated = new Concert(
                concertId, request.title(), request.artist(), request.description(),
                request.venue(), request.posterUrl(), request.thumbnailUrl(),
                request.startDate(), request.endDate(),
                request.maxTicketsPerPerson(), existing.getStatus()
        );
        for (var schedule : existing.getSchedules()) {
            updated.addSchedule(schedule);
        }
        concertRepository.save(updated);

        return ConcertResponse.from(updated);
    }

    /**
     * 공연 목록 조회 (MongoDB에서 읽기)
     */
    public List<ConcertResponse> getConcerts(int page, int size) {
        return concertRepository.findAll(page, size).stream()
                .map(ConcertResponse::from)
                .toList();
    }

    /**
     * 공연 상세 조회
     */
    public ConcertResponse getConcert(Long concertId) {
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 콘서트입니다."));
        return ConcertResponse.from(concert);
    }

    /**
     * 스케줄 추가 (dual-write)
     */
    @Transactional
    public ConcertResponse addSchedule(Long concertId, ScheduleCreateRequest request) {
        // 콘서트 존재 확인
        concertRefRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 콘서트입니다."));

        // PostgreSQL: ScheduleRef 저장
        int totalSeats = countTotalSeats(request.seatGroups());
        ScheduleRef scheduleRef = new ScheduleRef(concertId, request.date(), request.time(), totalSeats);
        scheduleRef = scheduleRefRepository.save(scheduleRef);
        Long scheduleId = scheduleRef.getId();

        // PostgreSQL: Seat 생성
        createSeats(scheduleId, request.seatGroups());

        // MongoDB: Concert에 Schedule 추가
        Concert concert = concertRepository.findById(concertId)
                .orElseThrow(() -> new IllegalArgumentException("MongoDB에 콘서트가 존재하지 않습니다."));
        concert.addSchedule(new Schedule(
                scheduleId, concertId,
                request.date(), request.time(),
                totalSeats, totalSeats
        ));
        concertRepository.save(concert);

        // Redis: 잔여 좌석 카운터 초기화
        soldOutService.initRemainingSeats(scheduleId, totalSeats);

        return ConcertResponse.from(concert);
    }

    private void createSeats(Long scheduleId, List<SeatGroupRequest> seatGroups) {
        for (SeatGroupRequest group : seatGroups) {
            for (int i = 1; i <= group.count(); i++) {
                String seatNumber = group.seatPrefix() + "-" + i;
                seatRepository.save(new Seat(scheduleId, seatNumber, group.grade(), group.price()));
            }
        }
    }

    private int countTotalSeats(List<SeatGroupRequest> seatGroups) {
        return seatGroups.stream().mapToInt(SeatGroupRequest::count).sum();
    }
}
