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
        ConcertListResponse.ConcertItem item = new ConcertListResponse.ConcertItem(
                1L, "10cm 콘서트", "10cm", "https://example.com/thumbnail.jpg",
                "올림픽공원", "2025-08-01", "2025-08-02", ConcertStatus.OPEN
        );

        ConcertListResponse response = new ConcertListResponse(List.of(item), page, size, 50L, 5, true);

        return ResponseEntity.ok(response);
    }

    // 콘서트 상세 조회 - Public
    @GetMapping("/{concertId}")
    public ResponseEntity<ConcertDetailResponse> getConcert(@PathVariable Long concertId) {
        ConcertDetailResponse.ScheduleItem schedule = new ConcertDetailResponse.ScheduleItem(
                1L, "2025-08-01", "19:00", 500, 120
        );

        ConcertDetailResponse response = new ConcertDetailResponse(
                concertId, "10cm 콘서트", "10cm", "공연 설명",
                "올림픽공원", "https://example.com/poster.jpg",
                List.of(schedule), 2, ConcertStatus.OPEN
        );

        return ResponseEntity.ok(response);
    }
}
