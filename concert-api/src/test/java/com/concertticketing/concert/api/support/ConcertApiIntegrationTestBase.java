package com.concertticketing.concert.api.support;

import com.concertticketing.domain.concert.entity.Concert;
import com.concertticketing.domain.concert.entity.ConcertStatus;
import com.concertticketing.domain.concert.repository.ConcertRepository;
import com.concertticketing.domain.schedule.entity.Schedule;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalTime;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class ConcertApiIntegrationTestBase {

    @Container
    @ServiceConnection
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7");

    @LocalServerPort
    protected int port;

    @Autowired
    protected ConcertRepository concertRepository;

    protected WebTestClient webTestClient;

    @BeforeEach
    void setUpBase() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
        concertRepository.deleteAll();
    }

    protected Concert seedConcert(Long id, String title) {
        Concert concert = new Concert(id, title, "아티스트", "설명", "장소",
                null, null, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2),
                2, ConcertStatus.OPEN);
        concert.addSchedule(new Schedule(id * 10 + 1, id,
                LocalDate.of(2026, 8, 1), LocalTime.of(19, 0), 6, 6));
        return concertRepository.save(concert);
    }
}
