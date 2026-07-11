package com.concertticketing.domain.notification.event;

/**
 * 알림 발송 최종 실패의 유형.
 */
public enum NotificationFailureType {

    /** 일시적 실패가 재시도까지 소진됨. */
    TRANSIENT_EXHAUSTED,

    /** 영구적 실패 — 재시도하지 않음. */
    PERMANENT
}
