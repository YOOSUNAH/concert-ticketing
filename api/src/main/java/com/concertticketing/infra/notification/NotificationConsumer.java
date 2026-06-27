package com.concertticketing.infra.notification;

import com.concertticketing.domain.notification.ProcessedEventGuard;
import com.concertticketing.domain.notification.exception.NotificationPermanentException;
import com.concertticketing.domain.notification.service.NotificationService;
import com.concertticketing.domain.payment.event.PaymentConfirmedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * 결제 완료 토픽을 구독해 알림을 발송하는 Kafka 컨슈머. (재시도·실패 처리 포함)
 *
 * <p><b>재시도(비차단)</b>: {@link RetryableTopic}이 실패한 메시지를 재시도 전용 토픽
 * ({@code payment.confirmed-retry-0/-1 ...})으로 옮겨 backoff 뒤 다시 소비한다. 예전의 수동
 * {@code Thread.sleep} 루프와 달리 <b>대기 동안 컨슈머 스레드를 점유하지 않는다.</b>
 *
 * <p><b>실패 분기</b>:
 * <ul>
 *   <li>일시적 실패({@code NotificationTransientException}) → backoff 재시도, 소진 시 DLT</li>
 *   <li>영구 실패({@link NotificationPermanentException}) → {@code exclude}로 <b>재시도 없이 DLT 직행</b></li>
 * </ul>
 *
 * <p><b>DLT(Dead Letter Topic)</b>: 최종 실패 메시지는 {@code payment.confirmed-dlt}에 적재된다.
 * 인메모리로 사라지던 예전 실패 이벤트와 달리, <b>브로커에 남아 재처리·감사·경보가 가능</b>하다.
 *
 * <p><b>멱등</b>: at-least-once라 같은 이벤트가 또 올 수 있다. 발송 성공한 eventId는 건너뛴다.
 * 성공 후에만 기록하므로(실패는 미기록) 재시도 메시지는 정상적으로 다시 처리된다.
 */
@Component
public class NotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final NotificationService notificationService;
    private final ProcessedEventGuard processedEventGuard;

    public NotificationConsumer(NotificationService notificationService,
                                ProcessedEventGuard processedEventGuard) {
        this.notificationService = notificationService;
        this.processedEventGuard = processedEventGuard;
    }

    @RetryableTopic(
            attempts = "${notification.retry.max-attempts:3}",
            backOff = @BackOff(
                    delayString = "${notification.retry.base-backoff-millis:500}",
                    multiplier = 2.0),
            exclude = NotificationPermanentException.class, // 영구 실패는 재시도 없이 DLT로
            dltStrategy = DltStrategy.FAIL_ON_ERROR)
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

        // 발송 실패 시 예외가 그대로 전파되어 RetryableTopic이 재시도/DLT를 처리한다.
        notificationService.sendBookingConfirmed(event.userId(), event.bookingId(), event.amount());

        processedEventGuard.markProcessed(event.eventId());
        log.info("알림 발송 성공 - eventId={}, bookingId={}", event.eventId(), event.bookingId());
    }

    /**
     * 재시도까지 소진됐거나 영구 실패한 메시지의 종착지(DLT) 처리 — 경보·적재 지점.
     *
     * <p>지금은 ERROR 로그가 전부지만, 여기만 손대면 Slack·PagerDuty 경보, 실패 메트릭,
     * 실패 건 DB 적재 등을 컨슈머(발송) 로직 수정 없이 덧붙일 수 있다. (OCP)
     */
    @DltHandler
    public void handleDlt(PaymentConfirmedEvent event,
                          @Header(KafkaHeaders.EXCEPTION_MESSAGE) String errorMessage) {
        log.error("알림 발송 최종 실패 (DLT 적재) - eventId={}, bookingId={}, userId={}, 사유={}",
                event.eventId(), event.bookingId(), event.userId(), errorMessage);
    }
}
