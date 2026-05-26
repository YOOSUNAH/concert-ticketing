package com.concertticketing.admin.api.config;

import com.concertticketing.admin.api.service.AdminConcertService;
import com.concertticketing.domain.concert.repository.ConcertRefRepository;
import com.concertticketing.domain.concert.repository.ConcertRepository;
import com.concertticketing.domain.queue.repository.QueueRepository;
import com.concertticketing.domain.schedule.repository.ScheduleRefRepository;
import com.concertticketing.domain.seat.repository.SeatRepository;
import com.concertticketing.domain.soldout.SoldOutService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminBeanConfig {

    @Bean
    public SoldOutService soldOutService(QueueRepository queueRepository) {
        return new SoldOutService(queueRepository);
    }

    @Bean
    public AdminConcertService adminConcertService(ConcertRefRepository concertRefRepository,
                                                    ScheduleRefRepository scheduleRefRepository,
                                                    SeatRepository seatRepository,
                                                    ConcertRepository concertRepository,
                                                    SoldOutService soldOutService) {
        return new AdminConcertService(concertRefRepository, scheduleRefRepository,
                seatRepository, concertRepository, soldOutService);
    }
}
