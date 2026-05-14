package com.concertticketing.concert.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = "com.concertticketing")
@EnableCaching
public class ConcertApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConcertApiApplication.class, args);
    }
}
