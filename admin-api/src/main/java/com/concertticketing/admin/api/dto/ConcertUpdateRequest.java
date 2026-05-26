package com.concertticketing.admin.api.dto;

import java.time.LocalDate;

public record ConcertUpdateRequest(
        String title,
        String artist,
        String description,
        String venue,
        String posterUrl,
        String thumbnailUrl,
        LocalDate startDate,
        LocalDate endDate,
        int maxTicketsPerPerson
) {
}
