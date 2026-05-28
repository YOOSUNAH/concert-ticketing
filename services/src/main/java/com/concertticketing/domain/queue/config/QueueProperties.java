package com.concertticketing.domain.queue.config;

public class QueueProperties {

    private final int heartbeatTtlSeconds;
    private final int maxActiveCount;
    private final int activeExpireSeconds;
    private final int tokenTtlSeconds;

    public QueueProperties(int heartbeatTtlSeconds,
                           int maxActiveCount,
                           int activeExpireSeconds,
                           int tokenTtlSeconds) {
        this.heartbeatTtlSeconds = heartbeatTtlSeconds;
        this.maxActiveCount = maxActiveCount;
        this.activeExpireSeconds = activeExpireSeconds;
        this.tokenTtlSeconds = tokenTtlSeconds;
    }

    public int getHeartbeatTtlSeconds() {
        return heartbeatTtlSeconds;
    }

    public int getMaxActiveCount() {
        return maxActiveCount;
    }

    public int getActiveExpireSeconds() {
        return activeExpireSeconds;
    }

    public int getTokenTtlSeconds() {
        return tokenTtlSeconds;
    }

    public double waitPerPersonSeconds() {
        return (double) activeExpireSeconds / maxActiveCount;
    }
}
