package com.concertticketing.domain.payment.listener;

import com.concertticketing.domain.payment.event.PaymentConfirmedEvent;
import com.concertticketing.infra.payment.PaymentConfirmedEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 결제 완료 이벤트를 Kafka로 내보내는 다리(브리지).
 *

 * <p>발행자(PaymentService)는 Spring 트랜잭션 이벤트만 던지고 Kafka를 모른다. 이 리스너가
 * 그 이벤트를 받아 {@link PaymentConfirmedEventProducer}로 토픽에 올린다. (디커플링)

 *
 * <p>{@code AFTER_COMMIT}: 결제 트랜잭션이 실제로 커밋된 뒤에만 발행한다.
 * → 롤백된(=결제 실패한) 거래는 토픽에 올라가지 않는다(유령 알림 차단).
 *

 * <p>예전에는 여기서 알림 발송(재시도·복구)까지 직접 했지만, 이제 그 책임은 Kafka 컨슈머로 넘어갔다.
 * → 알림 게이트웨이 장애가 결제 응답 스레드에 닿지 않고, 알림 처리를 독립적으로 확장·재처리할 수 있다.
 *
 * <p>실패 격리: {@code @Async}는 더 이상 필요 없다 — 카프카 {@code send()}가 논블로킹이라
 * 커밋 스레드를 잡지 않는다. 발행 자체의 성공/실패는 프로듀서 콜백에서 기록한다.
 */
@Component
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final PaymentConfirmedEventProducer producer;

    public PaymentEventListener(PaymentConfirmedEventProducer producer) {
        this.producer = producer;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        try {
            producer.publish(event);
        } catch (Exception e) {
            // send() 직전의 동기 단계(직렬화·파티셔너 등)에서 난 예외만 여기로 온다.
            // 결제는 이미 커밋이라 되돌리지 않고(rollback 없음) 맥락과 함께 기록한다.
            log.error("결제 완료 이벤트 발행 시도 실패 - eventId={}, bookingId={}, userId={}",

                    event.eventId(), event.bookingId(), event.userId(), e);
        }
    }
}
