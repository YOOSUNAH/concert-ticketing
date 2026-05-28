package com.concertticketing.domain.queue.config;

public class QueueProperties {

    private final int heartbeatThresholdSeconds;
    private final int maxActiveCount;
    private final int activeExpireSeconds;

    public QueueProperties(int heartbeatThresholdSeconds,
                           int maxActiveCount,
                           int activeExpireSeconds) {
        this.heartbeatThresholdSeconds = heartbeatThresholdSeconds;
        this.maxActiveCount = maxActiveCount;
        this.activeExpireSeconds = activeExpireSeconds;
    }

    public int getHeartbeatThresholdSeconds() {
        return heartbeatThresholdSeconds;
    }

    public int getMaxActiveCount() {
        return maxActiveCount;
    }

    public int getActiveExpireSeconds() {
        return activeExpireSeconds;
    }

    public double waitPerPersonSeconds() {
        return (double) activeExpireSeconds / maxActiveCount;
    }
}
