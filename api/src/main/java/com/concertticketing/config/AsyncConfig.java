package com.concertticketing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync // @Async 활성화 (없으면 @Async가 조용히 무시됨)
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

    // 이벤트(알림 등) 후속처리 전용 스레드풀.
    // 요청 처리용 ioExecutor와 분리한다 — 느린 알림 게이트웨이가 ioExecutor를 점유하면
    // 정작 사용자 요청(좌석/예매 조회)이 굶주리기 때문. 부가처리는 별도 풀에서 격리한다.
    @Bean(name = "eventExecutor")
    public Executor eventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("event-async-");
        executor.initialize();
        return executor;
    }
}
