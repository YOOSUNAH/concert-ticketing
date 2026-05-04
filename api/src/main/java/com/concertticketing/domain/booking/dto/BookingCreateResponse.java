package com.concertticketing.domain.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class BookingCreateResponse {
    private List<Long> bookingIds;
    private int totalAmount;
}
