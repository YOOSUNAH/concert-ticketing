package com.concertticketing.domain.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmRequest {
    private Long bookingId;
    private String paymentKey;
    private String orderId;
    private int amount;
    private int pointUsed;
    private String paymentMethod;
}
