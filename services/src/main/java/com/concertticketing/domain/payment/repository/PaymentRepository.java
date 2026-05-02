package com.concertticketing.domain.payment.repository;

import com.concertticketing.domain.payment.entity.Payment;

public interface PaymentRepository {

    Payment save(Payment payment);
}
