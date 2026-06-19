package com.concertticketing.queue.api.config;

import com.concertticketing.domain.auth.jwt.JwtTokenProvider;
import com.concertticketing.domain.queue.config.QueueProperties;
import com.concertticketing.domain.queue.repository.QueueRepository;
import com.concertticketing.domain.queue.service.QueueService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(QueueProperties.class)
public class QueueApiBeanConfig {

    @Bean
    public Cache<String, JwtTokenProvider.CachedToken> jwtTokenCache(
            @Value("${jwt.validity-seconds}") long validitySeconds
    ) {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofSeconds(validitySeconds))
                .build();
    }

    @Bean
    public JwtTokenProvider jwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.validity-seconds}") long validitySeconds,
            Cache<String, JwtTokenProvider.CachedToken> jwtTokenCache
    ) {
        return new JwtTokenProvider(secret, validitySeconds, jwtTokenCache);
    }

    @Bean
    public QueueService queueService(QueueRepository queueRepository, QueueProperties queueProperties) {
        return new QueueService(queueRepository, queueProperties);
    }
}
