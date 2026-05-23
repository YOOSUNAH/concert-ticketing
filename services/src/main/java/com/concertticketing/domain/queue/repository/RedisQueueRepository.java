package com.concertticketing.domain.queue.repository;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
public class RedisQueueRepository implements QueueRepository {

    private final StringRedisTemplate redis;

    public RedisQueueRepository(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Set<Long> findActiveScheduleIds() {
        ScanOptions options = ScanOptions.scanOptions().match("queue:waiting:*").count(100).build();
        Set<Long> ids = new HashSet<>();
        try (Cursor<String> cursor = redis.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                String idStr = key.substring("queue:waiting:".length());
                try {
                    ids.add(Long.parseLong(idStr));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return ids;
    }

    // === WAITING ===

    @Override
    public void addToWaiting(Long scheduleId, Long userId, long score) {
        redis.opsForZSet().add(waitingKey(scheduleId), userId.toString(), score);
    }

    @Override
    public void removeFromWaiting(Long scheduleId, Long userId) {
        redis.opsForZSet().remove(waitingKey(scheduleId), userId.toString());
    }

    @Override
    public Long getWaitingRank(Long scheduleId, Long userId) {
        return redis.opsForZSet().rank(waitingKey(scheduleId), userId.toString());
    }

    @Override
    public List<Long> getTopWaiting(Long scheduleId, int count) {
        Set<String> members = redis.opsForZSet().range(waitingKey(scheduleId), 0, count - 1);
        if (members == null || members.isEmpty()) {
            return Collections.emptyList();
        }
        return members.stream().map(Long::valueOf).toList();
    }

    // === ACTIVE ===

    @Override
    public void addToActive(Long scheduleId, Long userId, long expireAt) {
        redis.opsForZSet().add(activeKey(scheduleId), userId.toString(), expireAt);
    }

    @Override
    public void removeFromActive(Long scheduleId, Long userId) {
        redis.opsForZSet().remove(activeKey(scheduleId), userId.toString());
    }

    @Override
    public boolean isActive(Long scheduleId, Long userId) {
        Double score = redis.opsForZSet().score(activeKey(scheduleId), userId.toString());
        return score != null;
    }

    @Override
    public long getActiveCount(Long scheduleId) {
        Long size = redis.opsForZSet().size(activeKey(scheduleId));
        return size == null ? 0 : size;
    }

    @Override
    public List<Long> removeExpiredActive(Long scheduleId, long now) {
        String key = activeKey(scheduleId);
        Set<ZSetOperations.TypedTuple<String>> expired =
                redis.opsForZSet().rangeByScoreWithScores(key, Double.NEGATIVE_INFINITY, now - 1);

        if (expired == null || expired.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> removedUserIds = expired.stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .filter(java.util.Objects::nonNull)
                .map(Long::valueOf)
                .toList();

        redis.opsForZSet().removeRangeByScore(key, Double.NEGATIVE_INFINITY, now - 1);
        return removedUserIds;
    }

    // === Heartbeat (타임스탬프 기반) ===

    @Override
    public void refreshHeartbeat(Long scheduleId, Long userId) {
        redis.opsForValue().set(heartbeatKey(scheduleId, userId), String.valueOf(System.currentTimeMillis()));
    }

    @Override
    public boolean isAlive(Long scheduleId, Long userId, int thresholdSeconds) {
        String lastPing = redis.opsForValue().get(heartbeatKey(scheduleId, userId));
        if (lastPing == null) return false;
        long elapsed = System.currentTimeMillis() - Long.parseLong(lastPing);
        return elapsed < thresholdSeconds * 1000L;
    }

    // === Token 매핑 ===

    @Override
    public void saveTokenMapping(String token, Long scheduleId, Long userId) {
        redis.opsForValue().set(tokenKey(token), scheduleId + ":" + userId);
    }

    @Override
    public String getTokenMapping(String token) {
        return redis.opsForValue().get(tokenKey(token));
    }

    @Override
    public void deleteTokenMapping(String token) {
        redis.delete(tokenKey(token));
    }

    // === 잔여 좌석 카운터 + 매진/큐종료 플래그 ===

    @Override
    public void initRemainingSeats(Long scheduleId, int total) {
        redis.opsForValue().set(remainingSeatsKey(scheduleId), String.valueOf(total));
    }

    @Override
    public long decreaseRemainingSeats(Long scheduleId, int count) {
        Long result = redis.opsForValue().decrement(remainingSeatsKey(scheduleId), count);
        return result == null ? 0 : result;
    }

    @Override
    public void increaseRemainingSeats(Long scheduleId, int count) {
        redis.opsForValue().increment(remainingSeatsKey(scheduleId), count);
    }

    @Override
    public void markSoldOut(Long scheduleId) {
        redis.opsForValue().set(soldOutKey(scheduleId), "1");
    }

    @Override
    public boolean isSoldOut(Long scheduleId) {
        return Boolean.TRUE.equals(redis.hasKey(soldOutKey(scheduleId)));
    }

    @Override
    public void removeSoldOut(Long scheduleId) {
        redis.delete(soldOutKey(scheduleId));
    }

    @Override
    public void deleteWaitingQueue(Long scheduleId) {
        redis.delete(waitingKey(scheduleId));
    }

    @Override
    public void markQueueClosed(Long scheduleId, String reason) {
        redis.opsForValue().set(closedKey(scheduleId), reason);
    }

    @Override
    public String getQueueClosedReason(Long scheduleId) {
        return redis.opsForValue().get(closedKey(scheduleId));
    }

    // === Keys ===

    private String waitingKey(Long scheduleId) {
        return "queue:waiting:" + scheduleId;
    }

    private String activeKey(Long scheduleId) {
        return "queue:active:" + scheduleId;
    }

    private String heartbeatKey(Long scheduleId, Long userId) {
        return "queue:hb:" + scheduleId + ":" + userId;
    }

    private String tokenKey(String token) {
        return "queue:token:" + token;
    }

    private String remainingSeatsKey(Long scheduleId) {
        return "schedule:" + scheduleId + ":remaining-seats";
    }

    private String soldOutKey(Long scheduleId) {
        return "schedule:" + scheduleId + ":sold_out";
    }

    private String closedKey(Long scheduleId) {
        return "queue:" + scheduleId + ":closed";
    }
}
