package com.concertticketing.queue.api.config;

import com.concertticketing.domain.auth.jwt.JwtTokenProvider;
import com.concertticketing.domain.queue.config.QueueProperties;
import com.concertticketing.domain.queue.repository.QueueRepository;
import com.concertticketing.domain.queue.service.QueueService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueueApiBeanConfig {

    @Bean
    public JwtTokenProvider jwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.validity-seconds}") long validitySeconds
    ) {
        return new JwtTokenProvider(secret, validitySeconds);
    }

    @Bean
    public QueueProperties queueProperties(
            @Value("${queue.heartbeat-ttl-seconds}") int heartbeatTtlSeconds,
            @Value("${queue.max-active-count}") int maxActiveCount,
            @Value("${queue.active-expire-seconds}") int activeExpireSeconds,
            @Value("${queue.token-ttl-seconds}") int tokenTtlSeconds
    ) {
        return new QueueProperties(heartbeatTtlSeconds, maxActiveCount,
                activeExpireSeconds, tokenTtlSeconds);
    }

    @Bean
    public QueueService queueService(QueueRepository queueRepository, QueueProperties queueProperties) {
        return new QueueService(queueRepository, queueProperties);
    }
}
