package com.concertticketing.domain.notification.service;

import com.concertticketing.domain.notification.sender.NotificationSender;

/**
 * 알림 발송 서비스 (POJO).
 *
 * <p>결제 완료 같은 도메인 이벤트의 "후속 발송 로직"을 담는다.
 * 어떤 이벤트가 자신을 부르는지 모르며, 결제 도메인에도 의존하지 않는다(중립 파라미터).
 *
 * <p>실제 채널 전송은 {@link NotificationSender}(포트)에 위임한다. 발송이 실패하면
 * sender가 던지는 예외(일시적/영구적)를 그대로 전파하며, 재시도·실패 처리 여부는
 * 상위(리스너/디스패처)가 결정한다.
 */
public class NotificationService {

    private final NotificationSender sender;

    public NotificationService(NotificationSender sender) {
        this.sender = sender;
    }

    public void sendBookingConfirmed(Long userId, Long bookingId, int amount) {
        sender.send(userId, bookingId, amount);
    }
}
