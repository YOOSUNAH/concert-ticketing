package com.concertticketing.domain.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SeatListResponse {
    private List<SeatItem> seats;
    private boolean soldOut;

    @Getter
    @AllArgsConstructor
    public static class SeatItem {
        private Long seatId;
        private String seatNumber;
        private String grade;
        private int price;
        private SeatStatus status;
    }
}
