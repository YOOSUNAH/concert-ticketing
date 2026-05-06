package com.concertticketing.domain.concert.controller;

import com.concertticketing.domain.concert.dto.ConcertDetailResponse;
import com.concertticketing.domain.concert.dto.ConcertListResponse;
import com.concertticketing.domain.concert.dto.ConcertStatus;
import com.concertticketing.domain.concert.dto.ConcertWithSchedules;
import com.concertticketing.domain.concert.entity.Concert;
import com.concertticketing.domain.concert.service.ConcertService;
import com.concertticketing.domain.schedule.entity.Schedule;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/concerts")
public class ConcertController {

    private final ConcertService concertService;

    public ConcertController(ConcertService concertService) {
        this.concertService = concertService;
    }

    // 콘서트 목록 조회 - Public
    @GetMapping
    public ResponseEntity<ConcertListResponse> getConcerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<Concert> concerts = concertService.getConcerts(page, size);
        long total = concertService.getTotalCount();
        int totalPages = (int) Math.ceil((double) total / size);
        boolean hasNext = page + 1 < totalPages;

        List<ConcertListResponse.ConcertItem> items = concerts.stream()
                .map(c -> new ConcertListResponse.ConcertItem(
                        c.getId(),
                        c.getTitle(),
                        c.getArtist(),
                        c.getThumbnailUrl(),
                        c.getVenue(),
                        c.getStartDate().toString(),
                        c.getEndDate().toString(),
                        ConcertStatus.valueOf(c.getStatus().name())
                ))
                .toList();

        return ResponseEntity.ok(
                new ConcertListResponse(items, page, size, total, totalPages, hasNext)
        );
    }

    // 콘서트 상세 조회 - Public
    @GetMapping("/{concertId}")
    public ResponseEntity<ConcertDetailResponse> getConcert(@PathVariable Long concertId) {
        ConcertWithSchedules detail = concertService.getConcertDetail(concertId);
        Concert concert = detail.getConcert();

        List<ConcertDetailResponse.ScheduleItem> schedules = detail.getSchedules().stream()
                .map(this::toScheduleItem)
                .toList();

        return ResponseEntity.ok(new ConcertDetailResponse(
                concert.getId(),
                concert.getTitle(),
                concert.getArtist(),
                concert.getDescription(),
                concert.getVenue(),
                concert.getPosterUrl(),
                schedules,
                concert.getMaxTicketsPerPerson(),
                ConcertStatus.valueOf(concert.getStatus().name())
        ));
    }

    private ConcertDetailResponse.ScheduleItem toScheduleItem(Schedule s) {
        return new ConcertDetailResponse.ScheduleItem(
                s.getId(),
                s.getDate().toString(),
                s.getTime().toString(),
                s.getTotalSeats(),
                s.getRemainingSeats()
        );
    }
}
