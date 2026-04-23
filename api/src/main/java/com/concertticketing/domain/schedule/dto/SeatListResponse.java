package com.concertticketing.domain.schedule.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SeatListResponse {
    private List<SeatItem> seats;

    @Getter
    @Builder
    public static class SeatItem {
        private Long seatId;
        private String seatNumber;
        private String grade;
        private int price;
        private SeatStatus status;
    }
}
