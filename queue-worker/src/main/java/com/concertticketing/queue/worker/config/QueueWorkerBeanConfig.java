package com.concertticketing.queue.worker.config;

import com.concertticketing.domain.queue.config.QueueProperties;
import com.concertticketing.domain.queue.repository.QueueRepository;
import com.concertticketing.domain.queue.service.QueueService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueueWorkerBeanConfig {

    @Bean
    public QueueProperties queueProperties(
            @Value("${queue.heartbeat-threshold-seconds}") int heartbeatThresholdSeconds,
            @Value("${queue.max-active-count}") int maxActiveCount,
            @Value("${queue.active-expire-seconds}") int activeExpireSeconds
    ) {
        return new QueueProperties(heartbeatThresholdSeconds, maxActiveCount, activeExpireSeconds);
    }

    @Bean
    public QueueService queueService(QueueRepository queueRepository, QueueProperties queueProperties) {
        return new QueueService(queueRepository, queueProperties);
    }
}
