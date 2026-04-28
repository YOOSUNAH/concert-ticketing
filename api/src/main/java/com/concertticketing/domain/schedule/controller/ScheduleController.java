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
        SeatListResponse.SeatItem seat = new SeatListResponse.SeatItem(
                101L, "A-1", "VIP", 121000, SeatStatus.AVAILABLE
        );
        SeatListResponse response = new SeatListResponse(List.of(seat));
        return ResponseEntity.ok(response);
    }
}
