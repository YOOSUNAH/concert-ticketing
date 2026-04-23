package com.concertticketing.domain.concert.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ConcertDetailResponse {
    private Long concertId;
    private String title;
    private String artist;
    private String description;
    private String venue;
    private String posterUrl;
    private List<ScheduleItem> schedules;
    private int maxTicketsPerPerson;
    private ConcertStatus status;

    @Getter
    @Builder
    public static class ScheduleItem {
        private Long scheduleId;
        private String date;
        private String time;
        private int totalSeats;
        private int remainingSeats;
    }
}
