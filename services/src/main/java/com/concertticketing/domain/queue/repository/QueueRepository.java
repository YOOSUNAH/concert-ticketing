package com.concertticketing.domain.queue.repository;

import java.util.List;
import java.util.Set;

public interface QueueRepository {

    /** WAITING 키가 존재하는 scheduleId 목록 (queue-worker 스케줄러용) */
    Set<Long> findActiveScheduleIds();


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

    // === Heartbeat (타임스탬프 기반) ===

    void refreshHeartbeat(Long scheduleId, Long userId);

    boolean isAlive(Long scheduleId, Long userId, int thresholdSeconds);

    // === Token 매핑 (UUID → scheduleId:userId) ===

    void saveTokenMapping(String token, Long scheduleId, Long userId);

    String getTokenMapping(String token);

    void deleteTokenMapping(String token);

    // === 잔여 좌석 카운터 + 매진/큐종료 플래그 ===

    /** 잔여 좌석 카운터 초기화 (Schedule 시드 또는 lazy init) */
    void initRemainingSeats(Long scheduleId, int total, java.time.LocalDate scheduleDate);

    /** 잔여 좌석 감소. 새 값 반환. 0 이하면 매진 신호 */
    long decreaseRemainingSeats(Long scheduleId, int count);

    /** 잔여 좌석 복구 (예매 취소/실패 시) */
    void increaseRemainingSeats(Long scheduleId, int count);

    /** 매진 플래그 set (api → queue-worker 통신 매개) */
    void markSoldOut(Long scheduleId);

    boolean isSoldOut(Long scheduleId);

    void removeSoldOut(Long scheduleId);

    /** 큐 강제 종료 (queue-worker가 매진 감지 시 호출) */
    void deleteWaitingQueue(Long scheduleId);

    /** 종료된 큐 표시 (queue-api 폴링 응답용) */
    void markQueueClosed(Long scheduleId, String reason);

    String getQueueClosedReason(Long scheduleId);
}
