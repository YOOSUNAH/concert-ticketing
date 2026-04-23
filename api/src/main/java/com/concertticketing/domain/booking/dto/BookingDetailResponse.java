package com.concertticketing.domain.booking.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BookingDetailResponse {
    private Long bookingId;
    private String bookingNumber;
    private String concertTitle;
    private String venue;
    private String date;
    private String time;
    private List<String> seats;
    private int totalAmount;
    private int paidAmount;
    private int pointUsed;
    private String paymentMethod;
    private String ticketType;
    private BookingStatus status;
    private String paidAt;
}
