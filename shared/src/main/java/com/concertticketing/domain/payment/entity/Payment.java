package com.concertticketing.domain.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long bookingId;

    @Column(nullable = false)
    private String paymentKey;

    @Column(nullable = false, unique = true)
    private String orderId;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false)
    private int pointUsed;

    @Column(nullable = false)
    private String paymentMethod;

    @Column(nullable = false)
    private LocalDateTime paidAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private Integer refundedAmount;

    private LocalDateTime refundedAt;

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
        this.status = PaymentStatus.PAID;
    }

    /**
     * 환불 처리
     * - PAID 상태에서만 가능
     * - 전액 환불 (실결제액 + 포인트사용액 합계)
     */
    public void refund() {
        if (this.status == PaymentStatus.REFUNDED) {
            throw new IllegalStateException("이미 환불된 결제입니다.");
        }
        this.status = PaymentStatus.REFUNDED;
        this.refundedAmount = this.amount + this.pointUsed;
        this.refundedAt = LocalDateTime.now();
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

    public PaymentStatus getStatus() {
        return status;
    }

    public Integer getRefundedAmount() {
        return refundedAmount;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }
}
