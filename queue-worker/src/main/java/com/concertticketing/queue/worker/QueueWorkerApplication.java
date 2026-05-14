package com.concertticketing.queue.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.concertticketing")
@EnableScheduling
public class QueueWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(QueueWorkerApplication.class, args);
    }
}
