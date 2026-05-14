package com.concertticketing.concert.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = "com.concertticketing")
@EnableMongoRepositories(basePackages = "com.concertticketing")
@EnableCaching
public class ConcertApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConcertApiApplication.class, args);
    }
}
