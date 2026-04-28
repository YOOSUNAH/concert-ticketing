package com.concertticketing.domain.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class BookingListResponse {
    private List<BookingItem> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;

    @Getter
    @AllArgsConstructor
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
