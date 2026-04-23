package com.concertticketing.domain.booking.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookingCancelResponse {
    private Long bookingId;
    private String bookingNumber;
    private BookingStatus status;
    private int cancelledAmount;
    private String cancelledAt;
}
