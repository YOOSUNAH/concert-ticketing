package com.concertticketing.domain.payment.listener;

import com.concertticketing.domain.notification.NotificationDispatcher;
import com.concertticketing.domain.payment.event.PaymentConfirmedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 결제 완료 이벤트 소비자(어댑터).
 *
 * <p>발행자(PaymentService)를 모른 채 {@link PaymentConfirmedEvent}만 구독한다. (디커플링)
 * 발송의 재시도·복구는 {@link NotificationDispatcher}에 위임하고, 이 클래스는
 * "언제/어떤 조건에 호출되는지"(Spring 연결)만 담당한다.
 *
 * <p>{@code AFTER_COMMIT}: 결제 트랜잭션이 실제로 커밋된 뒤에만 발화한다.
 * → 롤백된(=결제 실패한) 거래엔 알림이 가지 않는다(유령 알림 차단).
 *
 * <p>{@code @Async}: 발송을 전용 풀(eventExecutor)의 별도 스레드로 넘긴다.
 * → 결제 응답 스레드는 알림을 기다리지 않는다(응답 지연 차단).
 *
 * <p>알림 실패 처리: 일시적/영구적 실패는 디스패처(재시도+@Recover)가 흡수하므로
 * 보통 예외가 여기까지 오지 않는다. 그래도 분류되지 않은 예외가 새어 나올 수 있어
 * 마지막 그물로 여기서 잡아 기록한다 — 결제는 이미 커밋이라 되돌리지(rollback) 않는다.
 * (@Async void 예외는 호출자에게 전파되지 않아, 안 잡으면 추적이 어려워지기 때문)
 */
@Component
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final NotificationDispatcher dispatcher;

    public PaymentEventListener(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        try {
            dispatcher.dispatch(event);
        } catch (Exception e) {
            // 디스패처의 재시도/복구가 놓친 예외만 여기로 온다. 결제로 전파하지 않고(rollback 없음)
            // 추적 가능하게 맥락과 함께 기록한다.
            log.error("결제 완료 알림 처리 실패 - eventId={}, bookingId={}, userId={}",
                    event.eventId(), event.bookingId(), event.userId(), e);
        }
    }
}
