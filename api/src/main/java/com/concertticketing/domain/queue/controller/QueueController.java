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
        QueueEnterResponse response = new QueueEnterResponse(
                "550e8400-e29b-41d4-a716-446655440000", 3842, 192
        );
        return ResponseEntity.ok(response);
    }

    // 대기 순번 조회 - Private
    @GetMapping("/status")
    public ResponseEntity<QueueStatusResponse> getQueueStatus(@RequestParam String queueToken) {
        QueueStatusResponse response = new QueueStatusResponse(120, QueueStatus.WAITING, null);
        return ResponseEntity.ok(response);
    }
}
