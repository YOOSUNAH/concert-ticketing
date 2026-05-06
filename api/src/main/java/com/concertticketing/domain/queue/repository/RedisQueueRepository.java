package com.concertticketing.domain.queue.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Repository
public class RedisQueueRepository implements QueueRepository {

    private final StringRedisTemplate redis;

    public RedisQueueRepository(StringRedisTemplate redis) {
        this.redis = redis;
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

    // === Heartbeat ===

    @Override
    public void refreshHeartbeat(Long scheduleId, Long userId, int ttlSeconds) {
        redis.opsForValue().set(heartbeatKey(scheduleId, userId), "1", Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public boolean isAlive(Long scheduleId, Long userId) {
        return Boolean.TRUE.equals(redis.hasKey(heartbeatKey(scheduleId, userId)));
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
}
