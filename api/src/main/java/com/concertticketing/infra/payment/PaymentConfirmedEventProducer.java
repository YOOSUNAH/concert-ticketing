package com.concertticketing.infra.payment;

import com.concertticketing.domain.payment.event.PaymentConfirmedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 결제 완료 이벤트를 Kafka로 발행하는 프로듀서(인프라 어댑터).
 *
 * <p>도메인 이벤트({@link PaymentConfirmedEvent})를 {@code payment.confirmed} 토픽에 올린다.
 * 발행 이후의 처리(알림 발송·재시도·실패 적재)는 컨슈머 쪽 책임이라 여기서는 모른다. (디커플링)
 *
 * <p>파티션 키로 {@code bookingId}를 쓴다 → 같은 예매의 이벤트는 항상 같은 파티션에 들어가
 * 순서가 보장된다. 동시에 여러 예매는 여러 파티션으로 분산돼 컨슈머를 병렬 확장할 수 있다.
 *
 * <p>{@code send()}는 논블로킹(즉시 future 반환)이라 호출 스레드를 점유하지 않는다.
 * 발행 성공/실패는 콜백에서 로그로 남긴다. (브로커 다운 등 발행 실패는 acks=all+retries로 1차 방어)
 */
@Component
public class PaymentConfirmedEventProducer {

    private static final Logger log = LoggerFactory.getLogger(PaymentConfirmedEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public PaymentConfirmedEventProducer(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${notification.kafka.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(PaymentConfirmedEvent event) {
        String key = String.valueOf(event.bookingId());
        kafkaTemplate.send(topic, key, event).whenComplete((result, ex) -> {
            if (ex != null) {
                // 발행 실패 — 결제는 이미 커밋 상태. 여기서 되돌리지 않고 추적 가능하게 기록한다.
                // (유실을 0으로 만들려면 후속 단계에서 Outbox 패턴으로 보강한다.)
                log.error("결제 완료 이벤트 발행 실패 - topic={}, eventId={}, bookingId={}",
                        topic, event.eventId(), event.bookingId(), ex);
            } else {
                log.info("결제 완료 이벤트 발행 - topic={}, partition={}, offset={}, eventId={}, bookingId={}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        event.eventId(),
                        event.bookingId());
            }
        });
    }
}
