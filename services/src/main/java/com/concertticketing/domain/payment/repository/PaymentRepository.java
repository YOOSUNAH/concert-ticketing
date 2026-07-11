package com.concertticketing.domain.payment.repository;

import com.concertticketing.domain.payment.entity.Payment;

import java.util.Optional;

public interface PaymentRepository {

    Payment save(Payment payment);

    /**
     * bookingId로 결제 정보 조회
     * - 환불 처리 시 사용
     * - 예매 상세 조회 시 paidAmount/paymentMethod/paidAt 결합용으로도 사용 예정
     */
    Optional<Payment> findByBookingId(Long bookingId);
}
