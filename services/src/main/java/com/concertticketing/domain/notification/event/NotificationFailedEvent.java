package com.concertticketing.domain.notification.event;

/**
 * 알림 발송이 최종 실패했음을 알리는 결과 이벤트.
 *
 * <p>디스패처가 "재시도 소진" 또는 "영구 실패" 시 발행한다. 경보·메트릭·재처리(DLQ) 같은
 * 후속 반응은 이 이벤트를 구독해서 덧붙이며, 디스패처(발송 책임)는 수정하지 않는다. (OCP·팬아웃)
 *
 * @param eventId   원본 결제 완료 이벤트의 멱등성 키
 * @param bookingId 예매 ID
 * @param userId    사용자 ID
 * @param type      실패 유형(일시적 소진 / 영구)
 * @param reason    실패 사유(원인 예외 메시지)
 */
public record NotificationFailedEvent(
        String eventId,
        Long bookingId,
        Long userId,
        NotificationFailureType type,
        String reason
) {
}
