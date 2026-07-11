package com.concertticketing.domain.notification;

import com.concertticketing.domain.notification.event.NotificationFailedEvent;
import com.concertticketing.domain.notification.event.NotificationFailureType;
import com.concertticketing.domain.notification.exception.NotificationPermanentException;
import com.concertticketing.domain.notification.exception.NotificationTransientException;
import com.concertticketing.domain.notification.service.NotificationService;
import com.concertticketing.domain.payment.event.PaymentConfirmedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 알림 발송의 재시도/복구를 담당하는 디스패처.
 *
 * <p>리스너(PaymentEventListener)와 도메인 서비스(NotificationService) 사이에서
 * "실패하면 어떻게 할지"(재시도·복구)를 책임진다.
 *
 * <p>흐름:
 * <pre>
 * 알림 발송 시도
 *   ├─ 성공 → 기록하고 끝
 *   └─ 실패 → 일시적이면 재시도(지수 backoff, 최대 maxAttempts회) → 소진되면 실패 기록
 *            └─ 영구적이면 재시도 없이 즉시 실패 기록
 * </pre>
 * 결제는 이미 커밋된 상태이므로 어떤 경우에도 되돌리지(rollback) 않는다. 실패는 기록으로 남긴다.
 *
 * <p>재시도는 외부 라이브러리(spring-retry) 없이 수동 루프로 구현한다(의존성 추가 0).
 * backoff 대기는 호출 스레드(eventExecutor)를 점유하므로 maxAttempts/간격을 작게 유지한다.
 */
@Component
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final int maxAttempts;
    private final long baseBackoffMillis;

    public NotificationDispatcher(
            NotificationService notificationService,
            ApplicationEventPublisher eventPublisher,
            @Value("${notification.retry.max-attempts:3}") int maxAttempts,
            @Value("${notification.retry.base-backoff-millis:500}") long baseBackoffMillis) {
        this.notificationService = notificationService;
        this.eventPublisher = eventPublisher;
        this.maxAttempts = maxAttempts;
        this.baseBackoffMillis = baseBackoffMillis;
    }

    /**
     * 알림 발송 진입점. 성공/실패 분기와 복구(기록)를 여기서 마무리한다.
     * (예외를 밖으로 던지지 않는다 — 결제 흐름과 격리)
     */
    public void dispatch(PaymentConfirmedEvent event) {
        try {
            sendWithRetry(event);
        } catch (NotificationPermanentException e) {
            publishFailure(event, NotificationFailureType.PERMANENT, e);
        } catch (NotificationTransientException e) {
            publishFailure(event, NotificationFailureType.TRANSIENT_EXHAUSTED, e);
        }
    }

    /**
     * 일시적 실패면 backoff 후 재시도, 영구적 실패면 즉시 위로 전파한다.
     * 재시도까지 소진되면 마지막 일시적 예외를 던진다.
     */
    private void sendWithRetry(PaymentConfirmedEvent event) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                notificationService.sendBookingConfirmed(event.userId(), event.bookingId(), event.amount());
                log.info("알림 발송 성공 - eventId={}, bookingId={}, attempt={}",
                        event.eventId(), event.bookingId(), attempt);
                return;
            } catch (NotificationTransientException e) {
                if (attempt >= maxAttempts) {
                    throw e; // 소진 → dispatch가 기록
                }
                long backoff = baseBackoffMillis * (1L << (attempt - 1)); // 500 → 1000 → 2000ms
                log.warn("알림 일시적 실패 - 재시도 예정 attempt={}/{}, eventId={}, 대기={}ms, 사유={}",
                        attempt, maxAttempts, event.eventId(), backoff, e.getMessage());
                sleep(backoff);
            }
            // NotificationPermanentException은 여기서 잡지 않음 → 즉시 전파되어 재시도 없이 dispatch가 기록
        }
    }

    private void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new NotificationTransientException("재시도 대기 중 인터럽트", ie);
        }
    }

    /**
     * 최종 실패를 결과 이벤트로 발행한다. (rollback 아님 — 결제는 성공 유지)
     * 경보·메트릭·DLQ 같은 후속 반응은 이 이벤트를 구독해 덧붙인다(디스패처는 수정 불필요).
     */
    private void publishFailure(PaymentConfirmedEvent event, NotificationFailureType type, Exception cause) {
        eventPublisher.publishEvent(new NotificationFailedEvent(
                event.eventId(), event.bookingId(), event.userId(), type, cause.getMessage()));
    }
}
