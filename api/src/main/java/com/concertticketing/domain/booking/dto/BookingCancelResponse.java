package com.concertticketing.domain.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BookingCancelResponse {
    private Long bookingId;
    private String bookingNumber;
    private BookingStatus status;
    private int cancelledAmount;
    private String cancelledAt;
}
