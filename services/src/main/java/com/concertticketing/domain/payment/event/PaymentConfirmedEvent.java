package com.concertticketing.domain.payment.event;

/**
 * 결제가 확정(커밋)되었음을 알리는 도메인 이벤트.
 *
 * <p>발행자(PaymentService)는 이 이벤트를 던지기만 하고 누가 소비하는지 모른다.
 * 소비자(알림/적립/통계 등)는 PaymentService를 모른 채 이 이벤트만 구독한다. (디커플링)
 *
 * <p>Spring 4.2+ 부터 임의 객체를 이벤트로 발행할 수 있어 {@code ApplicationEvent}를
 * 상속하지 않는다. services 모듈의 순수 자바(POJO) 원칙을 유지하기 위함.
 *
 * @param eventId   멱등성 키. 발행 시점에 생성(UUID). 동일 이벤트 중복 처리 방지에 사용.
 * @param bookingId 결제가 확정된 예매 ID
 * @param userId    결제한 사용자 ID
 * @param amount    실결제 금액(포인트 제외)
 * @param pointUsed 사용한 포인트
 */
public record PaymentConfirmedEvent(
        String eventId,
        Long bookingId,
        Long userId,
        int amount,
        int pointUsed
) {
}
