package com.concertticketing.domain.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BookingCreateResponse {
    private Long bookingId;
    private int totalAmount;
    private String bookerName;
}
