package com.concertticketing.domain.notification;

/**
 * 이미 처리한 이벤트인지 가려내는 멱등성 가드(포트).
 *
 * <p>Kafka는 at-least-once 전달이라 같은 이벤트가 두 번 올 수 있다(컨슈머 재시작·리밸런스 등).
 * 같은 결제 알림을 두 번 보내지 않도록, 발송에 "성공한" eventId를 기억해 두고 중복 전달을 거른다.
 *
 * <p>중요: 처리 <b>성공 후</b>에만 {@link #markProcessed}로 기록한다. 실패한 이벤트는 기록하지 않아야
 * 재시도(재전달)가 다시 흘러갈 수 있다. (성공 기록 → 진짜 중복만 스킵)
 *
 * <p>구현체는 인프라에 둔다. 지금은 단일 인스턴스용 인메모리(Caffeine)지만,
 * 여러 인스턴스에서 공유하려면 Redis 등 외부 저장소로 교체한다(인터페이스 유지).
 */
public interface ProcessedEventGuard {

    /** 이미 처리(발송 성공)된 eventId면 true. */
    boolean isAlreadyProcessed(String eventId);

    /** 처리 성공한 eventId를 기록한다. */
    void markProcessed(String eventId);
}
