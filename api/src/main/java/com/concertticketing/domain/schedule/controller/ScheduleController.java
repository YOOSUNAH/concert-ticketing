package com.concertticketing.domain.schedule.controller;

import com.concertticketing.domain.schedule.dto.SeatListResponse;
import com.concertticketing.domain.schedule.dto.SeatStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/schedules")
public class ScheduleController {

    // 잔여 좌석 조회 - Private
    @GetMapping("/{scheduleId}/seats")
    public ResponseEntity<SeatListResponse> getSeats(@PathVariable Long scheduleId) {
        SeatListResponse.SeatItem seat = SeatListResponse.SeatItem.builder()
                .seatId(101L)
                .seatNumber("A-1")
                .grade("VIP")
                .price(121000)
                .status(SeatStatus.AVAILABLE)
                .build();

        SeatListResponse response = SeatListResponse.builder()
                .seats(List.of(seat))
                .build();

        return ResponseEntity.ok(response);
    }
}
