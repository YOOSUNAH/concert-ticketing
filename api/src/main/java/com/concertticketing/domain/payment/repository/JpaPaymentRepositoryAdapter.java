package com.concertticketing.domain.payment.repository;

import com.concertticketing.domain.payment.entity.Payment;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaPaymentRepositoryAdapter implements PaymentRepository {

    private final SpringDataPaymentRepository delegate;

    public JpaPaymentRepositoryAdapter(SpringDataPaymentRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Payment save(Payment payment) {
        return delegate.save(payment);
    }

    @Override
    public Optional<Payment> findByBookingId(Long bookingId) {
        return delegate.findByBookingId(bookingId);
    }
}
