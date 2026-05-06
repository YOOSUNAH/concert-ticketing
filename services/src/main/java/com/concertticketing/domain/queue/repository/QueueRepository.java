package com.concertticketing.domain.queue.repository;

import java.util.List;

public interface QueueRepository {

    // === WAITING 그룹 (Sorted Set, score = 입장시각) ===

    void addToWaiting(Long scheduleId, Long userId, long score);

    void removeFromWaiting(Long scheduleId, Long userId);

    /** 내 순번 조회 (0-based → Service에서 +1) */
    Long getWaitingRank(Long scheduleId, Long userId);

    /** 앞쪽 N명 꺼내기 */
    List<Long> getTopWaiting(Long scheduleId, int count);

    // === ACTIVE 그룹 (Sorted Set, score = 만료시각) ===

    void addToActive(Long scheduleId, Long userId, long expireAt);

    void removeFromActive(Long scheduleId, Long userId);

    boolean isActive(Long scheduleId, Long userId);

    long getActiveCount(Long scheduleId);

    /** 만료된 ACTIVE 멤버 제거 (score < 현재시각) */
    List<Long> removeExpiredActive(Long scheduleId, long now);

    // === Heartbeat (TTL 기반) ===

    void refreshHeartbeat(Long scheduleId, Long userId, int ttlSeconds);

    boolean isAlive(Long scheduleId, Long userId);
}
