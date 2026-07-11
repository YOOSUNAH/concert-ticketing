package com.concertticketing.infra.notification;

import com.concertticketing.domain.notification.ProcessedEventGuard;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 멱등성 가드의 인메모리 구현 (Caffeine).
 *
 * <p>처리 성공한 eventId를 일정 시간/개수만큼 기억한다. 메모리 누수 방지를 위해
 * 만료(시간)와 최대 크기(개수) 상한을 둔다 — 오래된 이벤트는 어차피 재전달되지 않는다.
 *
 * <p>한계: 인스턴스 로컬 캐시라 여러 인스턴스가 같은 파티션을 (리밸런스 순간) 번갈아 잡으면
 * 중복을 완벽히 막지는 못한다. 컨슈머 그룹상 한 파티션은 한 인스턴스가 소유하므로 평상시엔 충분하다.
 * 엄격한 전역 멱등이 필요하면 이 구현만 Redis(SETNX)로 교체한다.
 */
@Component
public class CaffeineProcessedEventGuard implements ProcessedEventGuard {

    private final Cache<String, Boolean> processed = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterWrite(Duration.ofHours(6))
            .build();

    @Override
    public boolean isAlreadyProcessed(String eventId) {
        return processed.getIfPresent(eventId) != null;
    }

    @Override
    public void markProcessed(String eventId) {
        processed.put(eventId, Boolean.TRUE);
    }
}
