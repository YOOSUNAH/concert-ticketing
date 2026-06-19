package com.concertticketing.domain.payment.listener;

import com.concertticketing.domain.notification.service.NotificationService;
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
    NotificationService notificationService;

    @InjectMocks
    PaymentEventListener listener;

    @Test
    @DisplayName("결제 완료 이벤트를 받으면 알림 발송으로 위임한다")
    void onPaymentConfirmed_delegatesToNotification() {
        PaymentConfirmedEvent event = new PaymentConfirmedEvent(
                "evt-1", 999L, 1L, 237000, 5000);

        listener.onPaymentConfirmed(event);

        verify(notificationService).sendBookingConfirmed(1L, 999L, 237000);
    }
}
