package com.concertticketing.concert.api.config;

import com.concertticketing.domain.concert.repository.ConcertRepository;
import com.concertticketing.domain.concert.service.ConcertService;
import com.concertticketing.domain.schedule.repository.ScheduleRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConcertApiBeanConfig {

    @Bean
    public ScheduleRepository scheduleRepository(ConcertRepository concertRepository) {
        return new ScheduleRepository(concertRepository);
    }

    @Bean
    public ConcertService concertService(ConcertRepository concertRepository,
                                         ScheduleRepository scheduleRepository) {
        return new ConcertService(concertRepository, scheduleRepository);
    }
}
