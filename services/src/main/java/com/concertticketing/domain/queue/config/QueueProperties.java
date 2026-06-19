package com.concertticketing.domain.queue.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "queue")
public class QueueProperties {

    private int heartbeatThresholdSeconds;
    private int maxActiveCount;
    private int activeExpireSeconds;

    public double waitPerPersonSeconds() {
        return (double) activeExpireSeconds / maxActiveCount;
    }
}
