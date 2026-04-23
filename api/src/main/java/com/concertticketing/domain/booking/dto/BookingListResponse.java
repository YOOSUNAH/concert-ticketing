package com.concertticketing.domain.booking.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BookingListResponse {
    private List<BookingItem> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;

    @Getter
    @Builder
    public static class BookingItem {
        private Long bookingId;
        private String concertTitle;
        private String date;
        private String time;
        private List<String> seats;
        private int totalAmount;
        private BookingStatus status;
    }
}
