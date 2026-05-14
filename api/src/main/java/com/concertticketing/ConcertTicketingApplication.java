package com.concertticketing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories(basePackages = "com.concertticketing")
public class ConcertTicketingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConcertTicketingApplication.class, args);
    }

}
