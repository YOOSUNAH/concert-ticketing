package com.concertticketing.domain.payment.gateway;

public interface PaymentGateway {

    /**
     * PG사 결제 요청
     * 실패 시 PaymentGatewayException
     */
    void charge(String paymentKey, String orderId, int amount);
}
