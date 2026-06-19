package com.concertticketing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    // blocking I/O(JDBC/Redis) 작업 전용 스레드풀.
    // CompletableFuture.supplyAsync에 Executor를 넘기지 않으면 ForkJoinPool.commonPool()이 쓰이는데,
    // commonPool은 CPU 코어 수 기반 공용 풀이라 blocking 작업을 올리면 풀이 막혀 앱 전체에 악영향을 준다.
    // 그래서 blocking 전용 풀을 분리한다.
    @Bean(name = "ioExecutor")
    public Executor ioExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("io-async-");
        executor.initialize();
        return executor;
    }
}
