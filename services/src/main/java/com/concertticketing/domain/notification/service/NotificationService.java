package com.concertticketing.domain.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 알림 발송 서비스 (POJO).
 *
 * <p>결제 완료 같은 도메인 이벤트의 "후속 발송 로직"을 담는다.
 * 어떤 이벤트가 자신을 부르는지 모르며, 결제 도메인에도 의존하지 않는다(중립 파라미터).
 *
 * <p>실제 SMS/이메일 게이트웨이 연동은 미구현 — 지금은 로그로 대체(Fake sender).
 */
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void sendBookingConfirmed(Long userId, Long bookingId, int amount) {
        // TODO: 실제 SMS/이메일 발송 연동 (지금은 로그)
        log.info("[알림] 예매 결제 완료 - userId={}, bookingId={}, amount={}", userId, bookingId, amount);
    }
}
