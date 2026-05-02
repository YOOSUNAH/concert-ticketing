package com.concertticketing.domain.payment.entity;

import java.time.LocalDateTime;

public class Payment {

    private Long id;
    private Long bookingId;
    private String paymentKey;
    private String orderId;
    private int amount;
    private int pointUsed;
    private String paymentMethod;
    private LocalDateTime paidAt;

    protected Payment() {
    }

    public Payment(Long bookingId, String paymentKey, String orderId,
                   int amount, int pointUsed, String paymentMethod) {
        this.bookingId = bookingId;
        this.paymentKey = paymentKey;
        this.orderId = orderId;
        this.amount = amount;
        this.pointUsed = pointUsed;
        this.paymentMethod = paymentMethod;
        this.paidAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public String getPaymentKey() {
        return paymentKey;
    }

    public String getOrderId() {
        return orderId;
    }

    public int getAmount() {
        return amount;
    }

    public int getPointUsed() {
        return pointUsed;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }
}
