package com.concertticketing.domain.booking.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookingCreateResponse {
    private Long bookingId;
    private int totalAmount;
    private String bookerName;
}
