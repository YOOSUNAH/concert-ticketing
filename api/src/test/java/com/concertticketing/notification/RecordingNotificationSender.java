package com.concertticketing.notification;

import com.concertticketing.domain.notification.exception.NotificationPermanentException;
import com.concertticketing.domain.notification.exception.NotificationTransientException;
import com.concertticketing.domain.notification.sender.NotificationSender;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 테스트용 알림 sender — 호출 횟수를 세고, 모드에 따라 성공/일시적 실패/영구 실패를 흉내 낸다.
 * Kafka 통합테스트에서 "정상 / 실패 / 장애" 시나리오를 결정적으로 재현하기 위한 도구.
 */
public class RecordingNotificationSender implements NotificationSender {

    public enum Mode {
        /** 항상 성공 */
        SUCCEED,
        /** 항상 일시적 실패(재시도 대상) */
        FAIL_TRANSIENT,
        /** 항상 영구 실패(재시도 안 함) */
        FAIL_PERMANENT,
        /** {@code succeedFromAttempt}회차부터 성공, 그 전엔 일시적 실패 */
        TRANSIENT_THEN_SUCCEED
    }

    private final AtomicInteger attempts = new AtomicInteger();
    private final AtomicInteger successes = new AtomicInteger();
    private volatile Mode mode = Mode.SUCCEED;
    private volatile int succeedFromAttempt = 1;

    @Override
    public void send(Long userId, Long bookingId, int amount) {
        int n = attempts.incrementAndGet();
        switch (mode) {
            case SUCCEED -> successes.incrementAndGet();
            case FAIL_PERMANENT -> throw new NotificationPermanentException("영구 실패(테스트)");
            case FAIL_TRANSIENT -> throw new NotificationTransientException("일시적 실패(테스트) attempt=" + n);
            case TRANSIENT_THEN_SUCCEED -> {
                if (n < succeedFromAttempt) {
                    throw new NotificationTransientException("일시적 실패(테스트) attempt=" + n);
                }
                successes.incrementAndGet();
            }
        }
    }

    public void configure(Mode mode) {
        this.mode = mode;
    }

    public void configureTransientThenSucceed(int succeedFromAttempt) {
        this.mode = Mode.TRANSIENT_THEN_SUCCEED;
        this.succeedFromAttempt = succeedFromAttempt;
    }

    public int attempts() {
        return attempts.get();
    }

    public int successes() {
        return successes.get();
    }

    public void reset() {
        attempts.set(0);
        successes.set(0);
        mode = Mode.SUCCEED;
        succeedFromAttempt = 1;
    }
}
