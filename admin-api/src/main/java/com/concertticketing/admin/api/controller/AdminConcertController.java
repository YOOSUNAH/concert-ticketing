package com.concertticketing.admin.api.controller;

import com.concertticketing.admin.api.dto.ConcertCreateRequest;
import com.concertticketing.admin.api.dto.ConcertResponse;
import com.concertticketing.admin.api.dto.ConcertUpdateRequest;
import com.concertticketing.admin.api.dto.ScheduleCreateRequest;
import com.concertticketing.admin.api.service.AdminConcertService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/concerts")
public class AdminConcertController {

    private final AdminConcertService adminConcertService;

    public AdminConcertController(AdminConcertService adminConcertService) {
        this.adminConcertService = adminConcertService;
    }

    @PostMapping
    public ResponseEntity<ConcertResponse> createConcert(@RequestBody ConcertCreateRequest request) {
        ConcertResponse response = adminConcertService.createConcert(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{concertId}")
    public ResponseEntity<ConcertResponse> updateConcert(@PathVariable Long concertId,
                                                          @RequestBody ConcertUpdateRequest request) {
        ConcertResponse response = adminConcertService.updateConcert(concertId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ConcertResponse>> getConcerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminConcertService.getConcerts(page, size));
    }

    @GetMapping("/{concertId}")
    public ResponseEntity<ConcertResponse> getConcert(@PathVariable Long concertId) {
        return ResponseEntity.ok(adminConcertService.getConcert(concertId));
    }

    @PostMapping("/{concertId}/schedules")
    public ResponseEntity<ConcertResponse> addSchedule(@PathVariable Long concertId,
                                                        @RequestBody ScheduleCreateRequest request) {
        ConcertResponse response = adminConcertService.addSchedule(concertId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
