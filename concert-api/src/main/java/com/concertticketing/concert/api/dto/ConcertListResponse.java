package com.concertticketing.concert.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ConcertListResponse {
    private List<ConcertItem> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;

    @Getter
    @AllArgsConstructor
    public static class ConcertItem {
        private Long concertId;
        private String title;
        private String artist;
        private String thumbnailUrl;
        private String venue;
        private String startDate;
        private String endDate;
        private ConcertStatus status;
    }
}
