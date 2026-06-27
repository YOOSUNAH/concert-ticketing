package com.concertticketing.notification;

import com.concertticketing.domain.payment.event.PaymentConfirmedEvent;
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
 * Kafka 발행→소비 스모크 체크 (정상 경로).
 *
 * <p>결제 완료 이벤트를 토픽에 올리면 컨슈머가 받아 알림을 발송하는지를
 * 인메모리 브로커({@code @EmbeddedKafka})로 확인한다. Docker 불필요.
 */
@SpringBootTest(
        classes = KafkaNotificationTestApp.class,
        properties = {
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.kafka.consumer.group-id=notification-smoke",
                "notification.kafka.topic=payment.confirmed"
        })
@EmbeddedKafka(partitions = 1, topics = "payment.confirmed")
class NotificationKafkaSmokeTest {

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    RecordingNotificationSender sender;

    @Test
    @DisplayName("결제 완료 이벤트가 발행되면 컨슈머가 받아 알림을 발송한다")
    void paymentConfirmed_triggersNotification() {
        sender.reset();
        sender.configure(RecordingNotificationSender.Mode.SUCCEED);

        PaymentConfirmedEvent event = new PaymentConfirmedEvent("smoke-1", 100L, 1L, 50000, 0);
        kafkaTemplate.send("payment.confirmed", String.valueOf(event.bookingId()), event);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(sender.successes()).isEqualTo(1));
    }
}
