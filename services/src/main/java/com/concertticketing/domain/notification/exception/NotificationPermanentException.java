package com.concertticketing.domain.notification.exception;

/**
 * 영구적(permanent) 알림 실패 — 재시도해도 무의미한 종류.
 *
 * <p>예: 잘못된/차단된 수신자, 4xx 응답, 형식 오류. 즉시 실패로 처리한다.
 */
public class NotificationPermanentException extends NotificationException {

    public NotificationPermanentException(String message) {
        super(message);
    }

    public NotificationPermanentException(String message, Throwable cause) {
        super(message, cause);
    }
}
