package com.concertticketing.domain.queue.controller;

import com.concertticketing.domain.queue.dto.QueueEnterRequest;
import com.concertticketing.domain.queue.dto.QueueEnterResponse;
import com.concertticketing.domain.queue.dto.QueueStatus;
import com.concertticketing.domain.queue.dto.QueueStatusResponse;
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

    // 대기열 입장 - Private
    @PostMapping("/enter")
    public ResponseEntity<QueueEnterResponse> enterQueue(@RequestBody QueueEnterRequest request) {
        QueueEnterResponse response = QueueEnterResponse.builder()
                .queueToken("550e8400-e29b-41d4-a716-446655440000")
                .rank(3842)
                .estimatedWaitSeconds(192)
                .build();

        return ResponseEntity.ok(response);
    }

    // 대기 순번 조회 - Private
    @GetMapping("/status")
    public ResponseEntity<QueueStatusResponse> getQueueStatus(@RequestParam String queueToken) {
        QueueStatusResponse response = QueueStatusResponse.builder()
                .rank(120)
                .status(QueueStatus.WAITING)
                .admissionToken(null)
                .build();

        return ResponseEntity.ok(response);
    }
}
