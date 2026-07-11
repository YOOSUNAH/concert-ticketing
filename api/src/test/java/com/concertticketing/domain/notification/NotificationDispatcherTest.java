package com.concertticketing.domain.notification;

import com.concertticketing.domain.notification.event.NotificationFailedEvent;
import com.concertticketing.domain.notification.event.NotificationFailureType;
import com.concertticketing.domain.notification.exception.NotificationPermanentException;
import com.concertticketing.domain.notification.exception.NotificationTransientException;
import com.concertticketing.domain.notification.service.NotificationService;
import com.concertticketing.domain.payment.event.PaymentConfirmedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 알림 디스패처의 재시도/복구 동작 단위테스트.
 *
 * <p>backoff를 0으로 주입해 즉시 실행한다(대기 없이 재시도 횟수만 검증).
 */
@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    private static final int MAX_ATTEMPTS = 3;

    @Mock
    NotificationService notificationService;

    @Mock
    ApplicationEventPublisher eventPublisher;

    NotificationDispatcher dispatcher;

    final PaymentConfirmedEvent event = new PaymentConfirmedEvent("evt-1", 999L, 1L, 237000, 5000);

    @BeforeEach
    void setUp() {
        dispatcher = new NotificationDispatcher(notificationService, eventPublisher, MAX_ATTEMPTS, 0L);
    }

    @Test
    @DisplayName("발송 성공: 한 번에 보내고, 재시도·실패이벤트 없음")
    void success() {
        dispatcher.dispatch(event);

        verify(notificationService, times(1)).sendBookingConfirmed(1L, 999L, 237000);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("일시적 실패 후 성공: 재시도하다 성공하면 실패이벤트 없음")
    void transientThenSuccess() {
        doThrow(new NotificationTransientException("timeout"))
                .doThrow(new NotificationTransientException("timeout"))
                .doNothing()
                .when(notificationService).sendBookingConfirmed(anyLong(), anyLong(), anyInt());

        dispatcher.dispatch(event);

        verify(notificationService, times(3)).sendBookingConfirmed(1L, 999L, 237000);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("일시적 실패 지속: 최대 횟수만큼 시도 후 TRANSIENT_EXHAUSTED 이벤트 발행")
    void transientExhausted() {
        doThrow(new NotificationTransientException("timeout"))
                .when(notificationService).sendBookingConfirmed(anyLong(), anyLong(), anyInt());

        dispatcher.dispatch(event);

        verify(notificationService, times(MAX_ATTEMPTS)).sendBookingConfirmed(1L, 999L, 237000);

        ArgumentCaptor<NotificationFailedEvent> captor = ArgumentCaptor.forClass(NotificationFailedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(NotificationFailureType.TRANSIENT_EXHAUSTED, captor.getValue().type());
        assertEquals("evt-1", captor.getValue().eventId());
    }

    @Test
    @DisplayName("영구적 실패: 재시도 없이 1회만 시도 후 PERMANENT 이벤트 발행")
    void permanent() {
        doThrow(new NotificationPermanentException("invalid recipient"))
                .when(notificationService).sendBookingConfirmed(anyLong(), anyLong(), anyInt());

        dispatcher.dispatch(event);

        verify(notificationService, times(1)).sendBookingConfirmed(1L, 999L, 237000);

        ArgumentCaptor<NotificationFailedEvent> captor = ArgumentCaptor.forClass(NotificationFailedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(NotificationFailureType.PERMANENT, captor.getValue().type());
    }
}
