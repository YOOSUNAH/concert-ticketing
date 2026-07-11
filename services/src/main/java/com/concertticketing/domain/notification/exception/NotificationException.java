package com.concertticketing.domain.notification.exception;

/**
 * 알림 발송 실패의 공통 상위 예외.
 *
 * <p>구체 타입(일시적/영구적)으로 실패 성격을 구분해, 상위(리스너/디스패처)가
 * 재시도 여부를 결정할 수 있게 한다.
 */
public abstract class NotificationException extends RuntimeException {

    protected NotificationException(String message) {
        super(message);
    }

    protected NotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
