package com.concertticketing.infra.notification;

import com.concertticketing.domain.notification.NotificationDispatcher;
import com.concertticketing.domain.notification.ProcessedEventGuard;
import com.concertticketing.domain.payment.event.PaymentConfirmedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 결제 완료 토픽을 구독해 알림 발송을 트리거하는 Kafka 컨슈머.
 *
 * <p>{@code payment.confirmed} 토픽에서 {@link PaymentConfirmedEvent}를 받아
 * 발송 책임을 {@link NotificationDispatcher}에 위임한다. 발행자(결제)와는 토픽으로만 연결돼
 * 있어, 알림 처리가 느리거나 실패해도 결제 응답에 영향을 주지 않는다. (격리)
 *
 * <p>멱등 처리: at-least-once 전달이라 같은 이벤트가 두 번 올 수 있다. 이미 발송 성공한
 * eventId면 건너뛰고, 성공한 뒤에만 기록한다(실패는 기록 안 함 → 재전달 시 재처리 가능).
 *
 * <p>오프셋 커밋은 메서드가 정상 반환한 시점에 일어난다(ack-mode=record). 즉 처리에 성공해야
 * "읽음" 처리되므로, 컨슈머가 중간에 죽어도 마지막 미처리 이벤트부터 다시 읽는다(유실 방지).
 */
@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final NotificationDispatcher dispatcher;
    private final ProcessedEventGuard processedEventGuard;

    public NotificationConsumer(NotificationDispatcher dispatcher,
                                ProcessedEventGuard processedEventGuard) {
        this.dispatcher = dispatcher;
        this.processedEventGuard = processedEventGuard;
    }

    @KafkaListener(
            topics = "${notification.kafka.topic}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void consume(PaymentConfirmedEvent event) {
        if (processedEventGuard.isAlreadyProcessed(event.eventId())) {
            log.info("이미 처리된 결제 완료 이벤트 — 중복 스킵. eventId={}, bookingId={}",
                    event.eventId(), event.bookingId());
            return;
        }

        log.info("결제 완료 이벤트 수신 - eventId={}, bookingId={}, userId={}",
                event.eventId(), event.bookingId(), event.userId());

        dispatcher.dispatch(event);

        processedEventGuard.markProcessed(event.eventId());
    }
}
