package com.concertticketing.domain.concert.controller;

import com.concertticketing.domain.concert.dto.ConcertDetailResponse;
import com.concertticketing.domain.concert.dto.ConcertListResponse;
import com.concertticketing.domain.concert.dto.ConcertStatus;
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

    // 콘서트 목록 조회 - Public
    @GetMapping
    public ResponseEntity<ConcertListResponse> getConcerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        ConcertListResponse.ConcertItem item = ConcertListResponse.ConcertItem.builder()
                .concertId(1L)
                .title("10cm 콘서트")
                .artist("10cm")
                .thumbnailUrl("https://example.com/thumbnail.jpg")
                .venue("올림픽공원")
                .startDate("2025-08-01")
                .endDate("2025-08-02")
                .status(ConcertStatus.OPEN)
                .build();

        ConcertListResponse response = ConcertListResponse.builder()
                .content(List.of(item))
                .page(page)
                .size(size)
                .totalElements(50)
                .totalPages(5)
                .hasNext(true)
                .build();

        return ResponseEntity.ok(response);
    }

    // 콘서트 상세 조회 - Public
    @GetMapping("/{concertId}")
    public ResponseEntity<ConcertDetailResponse> getConcert(@PathVariable Long concertId) {
        ConcertDetailResponse.ScheduleItem schedule = ConcertDetailResponse.ScheduleItem.builder()
                .scheduleId(1L)
                .date("2025-08-01")
                .time("19:00")
                .totalSeats(500)
                .remainingSeats(120)
                .build();

        ConcertDetailResponse response = ConcertDetailResponse.builder()
                .concertId(concertId)
                .title("10cm 콘서트")
                .artist("10cm")
                .description("공연 설명")
                .venue("올림픽공원")
                .posterUrl("https://example.com/poster.jpg")
                .schedules(List.of(schedule))
                .maxTicketsPerPerson(2)
                .status(ConcertStatus.OPEN)
                .build();

        return ResponseEntity.ok(response);
    }
}
