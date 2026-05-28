package com.concertticketing.domain.queue.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "queue")
public class QueueProperties {

    private int heartbeatThresholdSeconds;
    private int maxActiveCount;
    private int activeExpireSeconds;

    public int getHeartbeatThresholdSeconds() {
        return heartbeatThresholdSeconds;
    }

    public void setHeartbeatThresholdSeconds(int heartbeatThresholdSeconds) {
        this.heartbeatThresholdSeconds = heartbeatThresholdSeconds;
    }

    public int getMaxActiveCount() {
        return maxActiveCount;
    }

    public void setMaxActiveCount(int maxActiveCount) {
        this.maxActiveCount = maxActiveCount;
    }

    public int getActiveExpireSeconds() {
        return activeExpireSeconds;
    }

    public void setActiveExpireSeconds(int activeExpireSeconds) {
        this.activeExpireSeconds = activeExpireSeconds;
    }

    public double waitPerPersonSeconds() {
        return (double) activeExpireSeconds / maxActiveCount;
    }
}
