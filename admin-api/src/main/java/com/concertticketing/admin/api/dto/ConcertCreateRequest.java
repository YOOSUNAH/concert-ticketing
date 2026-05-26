package com.concertticketing.admin.api.dto;

import java.time.LocalDate;
import java.util.List;

public record ConcertCreateRequest(
        String title,
        String artist,
        String description,
        String venue,
        String posterUrl,
        String thumbnailUrl,
        LocalDate startDate,
        LocalDate endDate,
        int maxTicketsPerPerson,
        List<ScheduleCreateRequest> schedules
) {
}
