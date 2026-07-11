package com.concertticketing.admin.api.dto;

public record SeatGroupRequest(
        String grade,
        String seatPrefix,
        int count,
        int price
) {
}
