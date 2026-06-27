package com.concertticketing.notification;

import com.concertticketing.domain.payment.event.PaymentConfirmedEvent;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 테스트 전용 DLT 관찰자.
 *
 * <p>최종 실패 메시지가 쌓이는 {@code payment.confirmed-dlt} 토픽을 운영 {@code @DltHandler}와
 * <b>다른 group-id</b>로 구독해, 같은 메시지를 독립적으로 한 부 받아 기록한다.
 * → 테스트에서 "DLT에 적재됐다"를 단언하는 수단.
 */
public class DltTestRecorder {

    private final List<PaymentConfirmedEvent> received = new CopyOnWriteArrayList<>();

    @KafkaListener(topics = "payment.confirmed-dlt", groupId = "dlt-test-recorder")
    public void onDlt(PaymentConfirmedEvent event) {
        received.add(event);
    }

    public List<PaymentConfirmedEvent> received() {
        return received;
    }

    public int count() {
        return received.size();
    }

    public void clear() {
        received.clear();
    }
}
