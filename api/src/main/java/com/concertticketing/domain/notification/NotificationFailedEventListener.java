package com.concertticketing.domain.notification;

import com.concertticketing.domain.notification.event.NotificationFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 알림 발송 최종 실패에 대한 경보(alert) 지점.
 *
 * <p>지금은 ERROR 로그로 남기는 것이 전부지만, 이 리스너만 추가/수정하면
 * Slack·PagerDuty 경보, 실패 메트릭 집계, 실패 건 DB 적재(DLQ) 등을
 * 디스패처(발송 책임) 수정 없이 덧붙일 수 있다. (OCP·팬아웃)
 */
@Component
public class NotificationFailedEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationFailedEventListener.class);

    @EventListener
    public void onNotificationFailed(NotificationFailedEvent event) {
        log.error("알림 발송 최종 실패 - type={}, eventId={}, bookingId={}, userId={}, 사유={}",
                event.type(), event.eventId(), event.bookingId(), event.userId(), event.reason());
    }
}
