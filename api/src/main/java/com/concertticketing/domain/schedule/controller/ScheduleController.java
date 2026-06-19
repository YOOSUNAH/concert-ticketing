package com.concertticketing.domain.schedule.controller;

import com.concertticketing.domain.queue.repository.QueueRepository;
import com.concertticketing.domain.schedule.dto.SeatListResponse;
import com.concertticketing.domain.schedule.dto.SeatStatus;
import com.concertticketing.domain.seat.entity.Seat;
import com.concertticketing.domain.seat.service.SeatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/schedules")
public class ScheduleController {

    private static final Logger log = LoggerFactory.getLogger(ScheduleController.class);

    private final SeatService seatService;
    private final QueueRepository queueRepository;
    private final Executor ioExecutor;

    public ScheduleController(SeatService seatService,
                             QueueRepository queueRepository,
                             Executor ioExecutor) {
        this.seatService = seatService;
        this.queueRepository = queueRepository;
        this.ioExecutor = ioExecutor;
    }

    // 잔여 좌석 조회 - Private (CompletableFuture 비동기 전환)
    @GetMapping("/{scheduleId}/seats")
    public CompletableFuture<ResponseEntity<SeatListResponse>> getSeats(@PathVariable Long scheduleId) {
        // [1] 톰캣 워커 스레드(http-nio-*). CompletableFuture를 반환하는 순간 이 스레드는 즉시 반납된다.
        log.info("[1] controller 진입 thread = {}", Thread.currentThread().getName());

        return CompletableFuture
                // (A) soldOut 여부 조회 - ioExecutor 스레드에서 실행
                .supplyAsync(() -> {
                    log.info("[2] isSoldOut 처리 thread = {}", Thread.currentThread().getName());
                    return queueRepository.isSoldOut(scheduleId);
                }, ioExecutor)
                // (B) 이전 결과(soldOut)에 의존해 다음 비동기 작업을 연결 (chain)
                .thenCompose(soldOut -> {
                    if (soldOut) {
                        return CompletableFuture.completedFuture(
                                new SeatListResponse(Collections.emptyList(), true));
                    }
                    return CompletableFuture.supplyAsync(() -> {
                        log.info("[3] getSeats(DB) 처리 thread = {}", Thread.currentThread().getName());
                        return toResponse(seatService.getSeats(scheduleId));
                    }, ioExecutor);
                })
                // (C) 최종 매핑
                .thenApply(body -> {
                    log.info("[4] 응답 조립 thread = {}", Thread.currentThread().getName());
                    return ResponseEntity.ok(body);
                });
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
