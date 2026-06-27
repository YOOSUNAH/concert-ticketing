package com.concertticketing.notification;

import com.concertticketing.domain.payment.event.PaymentConfirmedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * 결제 완료 → 알림 발송의 Kafka 통합 테스트. (인메모리 브로커 {@code @EmbeddedKafka}, Docker 불필요)
 *
 * <p>운영 코드(NotificationConsumer의 RetryableTopic+DLT, 멱등 가드)를 그대로 태우고,
 * 발송 채널만 {@link RecordingNotificationSender}로 바꿔 정상/실패/장애/멱등을 결정적으로 검증한다.
 *
 * <p>비동기(컨슈머는 별도 스레드)라 발행 직후 단언하면 이른다 → {@code Awaitility}로
 * "조건이 만족될 때까지" 반복 확인한다. 재시도 backoff는 테스트 프로퍼티로 짧게(50ms) 둔다.
 */
@SpringBootTest(
        classes = KafkaNotificationTestApp.class,
        properties = {
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.kafka.consumer.group-id=notification-itest",
                "notification.kafka.topic=payment.confirmed",
                "notification.retry.max-attempts=3",
                "notification.retry.base-backoff-millis=50"
        })
@EmbeddedKafka(partitions = 1, topics = "payment.confirmed")
class NotificationKafkaIntegrationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    RecordingNotificationSender sender;

    @Autowired
    DltTestRecorder dlt;

    @BeforeEach
    void resetState() {
        sender.reset();
        dlt.clear();
    }

    private void publish(PaymentConfirmedEvent event) {
        kafkaTemplate.send("payment.confirmed", String.valueOf(event.bookingId()), event);
    }

    @Test
    @DisplayName("정상: 결제 완료 이벤트가 발행되면 컨슈머가 받아 알림을 발송한다")
    void success_sendsNotification() {
        sender.configure(RecordingNotificationSender.Mode.SUCCEED);

        publish(new PaymentConfirmedEvent("itest-success-1", 100L, 1L, 50000, 0));

        await().atMost(TIMEOUT).untilAsserted(() ->
                assertThat(sender.successes()).isEqualTo(1));
        assertThat(sender.attempts()).isEqualTo(1); // 재시도 없음
        assertThat(dlt.count()).isZero();           // DLT 없음
    }

    @Test
    @DisplayName("실패(영구): 영구 실패는 재시도 없이 DLT로 직행한다")
    void permanentFailure_goesStraightToDlt() {
        sender.configure(RecordingNotificationSender.Mode.FAIL_PERMANENT);

        publish(new PaymentConfirmedEvent("itest-permanent-1", 200L, 2L, 30000, 0));

        await().atMost(TIMEOUT).untilAsserted(() ->
                assertThat(dlt.count()).isEqualTo(1));
        assertThat(sender.attempts()).isEqualTo(1); // exclude → 재시도 없음
        assertThat(sender.successes()).isZero();
    }

    @Test
    @DisplayName("장애(일시적·소진): 일시적 실패가 재시도까지 소진되면 DLT로 간다")
    void transientFailure_exhaustsRetriesThenDlt() {
        sender.configure(RecordingNotificationSender.Mode.FAIL_TRANSIENT);

        publish(new PaymentConfirmedEvent("itest-transient-exhaust-1", 300L, 3L, 30000, 0));

        await().atMost(TIMEOUT).untilAsserted(() ->
                assertThat(dlt.count()).isEqualTo(1));
        assertThat(sender.attempts()).isEqualTo(3); // 최초 1 + 재시도 2 = max-attempts
        assertThat(sender.successes()).isZero();
    }

    @Test
    @DisplayName("장애(일시적·복구): 재시도 중 성공하면 알림이 나가고 DLT로 가지 않는다")
    void transientFailure_recoversBeforeExhaustion() {
        sender.configureTransientThenSucceed(3); // 3회차 시도에서 성공

        publish(new PaymentConfirmedEvent("itest-transient-recover-1", 400L, 4L, 30000, 0));

        await().atMost(TIMEOUT).untilAsserted(() ->
                assertThat(sender.successes()).isEqualTo(1));
        assertThat(sender.attempts()).isEqualTo(3); // 1,2 실패 후 3에서 성공
        assertThat(dlt.count()).isZero();           // 복구됐으므로 DLT 없음
    }

    @Test
    @DisplayName("멱등: 같은 eventId가 두 번 전달돼도 알림은 한 번만 나간다")
    void duplicateDelivery_isHandledIdempotently() {
        sender.configure(RecordingNotificationSender.Mode.SUCCEED);
        PaymentConfirmedEvent event = new PaymentConfirmedEvent("itest-idem-1", 500L, 5L, 30000, 0);

        publish(event);
        publish(event); // 동일 eventId 중복 전달 (at-least-once 흉내)

        // pollDelay로 두 메시지가 모두 흘러갈 시간을 준 뒤에도 발송이 1회뿐인지 확인
        await().pollDelay(Duration.ofSeconds(2)).atMost(TIMEOUT).untilAsserted(() -> {
            assertThat(sender.successes()).isEqualTo(1);
            assertThat(sender.attempts()).isEqualTo(1); // 두 번째는 멱등 가드가 스킵
        });
        assertThat(dlt.count()).isZero();
    }
}
