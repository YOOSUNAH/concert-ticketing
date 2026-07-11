package com.concertticketing.queue.worker.config;

import com.concertticketing.domain.queue.config.QueueProperties;
import com.concertticketing.domain.queue.repository.QueueRepository;
import com.concertticketing.domain.queue.service.QueueService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(QueueProperties.class)
public class QueueWorkerBeanConfig {

    @Bean
    public QueueService queueService(QueueRepository queueRepository, QueueProperties queueProperties) {
        return new QueueService(queueRepository, queueProperties);
    }
}
