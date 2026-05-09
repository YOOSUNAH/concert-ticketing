package com.concertticketing.domain.queue.controller;

import com.concertticketing.auth.AuthUserId;
import com.concertticketing.domain.queue.dto.QueueEnterRequest;
import com.concertticketing.domain.queue.dto.QueueEnterResponse;
import com.concertticketing.domain.queue.dto.QueueEntryResult;
import com.concertticketing.domain.queue.dto.QueueStatus;
import com.concertticketing.domain.queue.dto.QueueStatusResponse;
import com.concertticketing.domain.queue.dto.QueueStatusResult;
import com.concertticketing.domain.queue.service.QueueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/queue")
public class QueueController {

    private final QueueService queueService;

    public QueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    // 대기열 입장 - Private
    @PostMapping("/enter")
    public ResponseEntity<QueueEnterResponse> enterQueue(
            @AuthUserId Long userId,
            @RequestBody QueueEnterRequest request
    ) {
        QueueEntryResult result = queueService.enterQueue(userId, request.getScheduleId());
        return ResponseEntity.ok(new QueueEnterResponse(
                result.getQueueToken(),
                Math.toIntExact(result.getRank()),
                result.getEstimatedWaitSeconds()
        ));
    }

    // 대기 순번 조회 - Private
    @GetMapping("/status")
    public ResponseEntity<QueueStatusResponse> getQueueStatus(
            @AuthUserId Long userId,
            @RequestParam String queueToken
    ) {
        QueueStatusResult result = queueService.getQueueStatus(userId, queueToken);
        return ResponseEntity.ok(new QueueStatusResponse(
                Math.toIntExact(result.getRank()),
                QueueStatus.valueOf(result.getStatus()),
                result.getAdmissionToken()
        ));
    }
}
