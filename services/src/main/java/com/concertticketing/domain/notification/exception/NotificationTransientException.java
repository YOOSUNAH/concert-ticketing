package com.concertticketing.domain.notification.exception;

/**
 * 일시적(transient) 알림 실패 — 재시도하면 성공할 수 있는 종류.
 *
 * <p>예: 게이트웨이 타임아웃, 5xx 응답, 일시적 네트워크 단절, 일시적 rate-limit.
 */
public class NotificationTransientException extends NotificationException {

    public NotificationTransientException(String message) {
        super(message);
    }

    public NotificationTransientException(String message, Throwable cause) {
        super(message, cause);
    }
}
