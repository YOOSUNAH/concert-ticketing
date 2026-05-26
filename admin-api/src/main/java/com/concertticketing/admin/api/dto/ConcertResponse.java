package com.concertticketing.admin.api.dto;

import com.concertticketing.domain.concert.entity.Concert;
import com.concertticketing.domain.concert.entity.ConcertStatus;
import com.concertticketing.domain.schedule.entity.Schedule;

import java.time.LocalDate;
import java.util.List;

public record ConcertResponse(
        Long id,
        String title,
        String artist,
        String venue,
        ConcertStatus status,
        LocalDate startDate,
        LocalDate endDate,
        int maxTicketsPerPerson,
        List<ScheduleResponse> schedules
) {

    public static ConcertResponse from(Concert concert) {
        List<ScheduleResponse> schedules = concert.getSchedules().stream()
                .map(ScheduleResponse::from)
                .toList();
        return new ConcertResponse(
                concert.getId(), concert.getTitle(), concert.getArtist(),
                concert.getVenue(), concert.getStatus(),
                concert.getStartDate(), concert.getEndDate(),
                concert.getMaxTicketsPerPerson(), schedules
        );
    }

    public record ScheduleResponse(Long id, LocalDate date, java.time.LocalTime time, int totalSeats) {
        public static ScheduleResponse from(Schedule schedule) {
            return new ScheduleResponse(schedule.getId(), schedule.getDate(), schedule.getTime(), schedule.getTotalSeats());
        }
    }
}
