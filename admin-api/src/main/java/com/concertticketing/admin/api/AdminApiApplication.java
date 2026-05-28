package com.concertticketing.admin.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.concertticketing.admin.api",
        "com.concertticketing.domain"
})
@AutoConfigurationPackage(basePackages = "com.concertticketing.domain")
@EnableJpaRepositories("com.concertticketing.admin.api.repository")
@EnableMongoRepositories("com.concertticketing.domain.concert.repository")
public class AdminApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminApiApplication.class, args);
    }
}
