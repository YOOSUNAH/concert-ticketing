package com.concertticketing.domain.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BookingCreateRequest {
    private Long scheduleId;
    private List<Long> seatIds;
    private String admissionToken;
}
