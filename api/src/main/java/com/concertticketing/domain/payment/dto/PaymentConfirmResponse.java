package com.concertticketing.domain.payment.dto;

import com.concertticketing.domain.booking.dto.BookingStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PaymentConfirmResponse {
    private String bookingNumber;
    private BookingStatus status;
    private List<String> seats;
    private String concertTitle;
    private String date;
    private String time;
    private int paidAmount;
    private String paidAt;
}
