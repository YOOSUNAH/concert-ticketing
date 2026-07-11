package com.concertticketing.domain.notification.sender;

/**
 * 알림 발송 채널 포트.
 *
 * <p>실제 SMS/이메일 게이트웨이 연동을 추상화한다. 구현체는 인프라(api 모듈)에 둔다.
 * 발송에 실패하면 실패 성격에 따라 다음 예외를 던진다.
 * <ul>
 *   <li>{@link com.concertticketing.domain.notification.exception.NotificationTransientException}
 *       — 일시적 실패(재시도 가능)</li>
 *   <li>{@link com.concertticketing.domain.notification.exception.NotificationPermanentException}
 *       — 영구적 실패(재시도 무의미)</li>
 * </ul>
 */
public interface NotificationSender {

    void send(Long userId, Long bookingId, int amount);
}
