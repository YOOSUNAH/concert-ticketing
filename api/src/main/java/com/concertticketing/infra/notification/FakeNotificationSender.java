package com.concertticketing.infra.notification;

import com.concertticketing.domain.notification.sender.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 알림 발송 포트의 임시 구현 — 실제 SMS/이메일 게이트웨이 연동 전까지 로그로 대체(Fake).
 *
 * <p>실제 게이트웨이를 붙일 때, 응답에 따라
 * {@link com.concertticketing.domain.notification.exception.NotificationTransientException}(타임아웃·5xx)
 * 또는 {@link com.concertticketing.domain.notification.exception.NotificationPermanentException}(4xx·잘못된 수신자)을
 * 던지도록 교체한다.
 */
public class FakeNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(FakeNotificationSender.class);

    @Override
    public void send(Long userId, Long bookingId, int amount) {
        // TODO: 실제 게이트웨이 연동 (지금은 로그)
        log.info("[알림] 예매 결제 완료 - userId={}, bookingId={}, amount={}", userId, bookingId, amount);
    }
}
