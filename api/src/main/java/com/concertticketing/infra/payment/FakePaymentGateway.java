package com.concertticketing.infra.payment;

import com.concertticketing.domain.payment.gateway.PaymentGateway;

public class FakePaymentGateway implements PaymentGateway {

    @Override
    public void charge(String paymentKey, String orderId, int amount) {
        // 항상 성공
    }
}
