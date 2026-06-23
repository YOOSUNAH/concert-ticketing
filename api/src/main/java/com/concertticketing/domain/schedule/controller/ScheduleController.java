package com.concertticketing.domain.schedule.controller;

import com.concertticketing.domain.queue.repository.QueueRepository;
import com.concertticketing.domain.schedule.dto.SeatListResponse;
import com.concertticketing.domain.schedule.dto.SeatStatus;
import com.concertticketing.domain.seat.entity.Seat;
import com.concertticketing.domain.seat.service.SeatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/schedules")
public class ScheduleController {

    private final SeatService seatService;
    private final QueueRepository queueRepository;

    public ScheduleController(SeatService seatService,
                             QueueRepository queueRepository) {
        this.seatService = seatService;
        this.queueRepository = queueRepository;
    }

    // 잔여 좌석 조회 - Private
    // soldOut 조회 → (매진이 아니면) 좌석 조회로 이어지는 순차 의존 흐름이라 병렬화할 작업이 없다.
    // CompletableFuture는 이득 없이 스레드 홉만 늘리므로 동기로 처리한다.
    @GetMapping("/{scheduleId}/seats")
    public ResponseEntity<SeatListResponse> getSeats(@PathVariable Long scheduleId) {
        if (queueRepository.isSoldOut(scheduleId)) {
            return ResponseEntity.ok(new SeatListResponse(Collections.emptyList(), true));
        }
        return ResponseEntity.ok(toResponse(seatService.getSeats(scheduleId)));
    }

    private SeatListResponse toResponse(List<Seat> seats) {
        List<SeatListResponse.SeatItem> items = seats.stream()
                .map(s -> new SeatListResponse.SeatItem(
                        s.getId(),
                        s.getSeatNumber(),
                        s.getGrade(),
                        s.getPrice(),
                        SeatStatus.valueOf(s.getStatus().name())
                ))
                .toList();

        return new SeatListResponse(items, false);
    }
}
