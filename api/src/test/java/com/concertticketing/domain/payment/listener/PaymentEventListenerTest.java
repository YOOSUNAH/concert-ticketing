package com.concertticketing.domain.payment.listener;

import com.concertticketing.domain.notification.NotificationDispatcher;
import com.concertticketing.domain.payment.event.PaymentConfirmedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

    @Mock
    NotificationDispatcher dispatcher;

    @InjectMocks
    PaymentEventListener listener;

    @Test
    @DisplayName("결제 완료 이벤트를 받으면 디스패처로 위임한다")
    void onPaymentConfirmed_delegatesToDispatcher() {
        PaymentConfirmedEvent event = new PaymentConfirmedEvent(
                "evt-1", 999L, 1L, 237000, 5000);

        listener.onPaymentConfirmed(event);

        verify(dispatcher).dispatch(event);
    }
}
