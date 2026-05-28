package com.concertticketing.domain.seat.lock;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collection;

@Repository
public class RedisSeatLockRepository implements SeatLockRepository {

    private final StringRedisTemplate redis;

    public RedisSeatLockRepository(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean tryAcquire(Long seatId, Long userId, int ttlSeconds) {
        Boolean ok = redis.opsForValue().setIfAbsent(
                key(seatId),
                userId.toString(),
                Duration.ofSeconds(ttlSeconds)
        );
        return Boolean.TRUE.equals(ok);
    }

    @Override
    public void release(Long seatId) {
        redis.delete(key(seatId));
    }

    @Override
    public void releaseAll(Collection<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            return;
        }
        seatIds.forEach(this::release);
    }

    private String key(Long seatId) {
        return "lock:seat:" + seatId;
    }
}
