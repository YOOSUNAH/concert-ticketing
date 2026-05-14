package com.concertticketing.config;

import com.concertticketing.domain.concert.entity.Concert;
import com.concertticketing.domain.concert.entity.ConcertStatus;
import com.concertticketing.domain.concert.repository.ConcertRepository;
import com.concertticketing.domain.schedule.entity.Schedule;
import com.concertticketing.domain.schedule.repository.ScheduleRepository;
import com.concertticketing.domain.seat.entity.Seat;
import com.concertticketing.domain.seat.repository.SeatRepository;
import com.concertticketing.domain.soldout.SoldOutService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalTime;

@Configuration
public class DataLoader {

    @Bean
    public ApplicationRunner seedData(ConcertRepository concertRepository,
                                      ScheduleRepository scheduleRepository,
                                      SeatRepository seatRepository,
                                      SoldOutService soldOutService) {
        return args -> {
            // 이미 데이터 있으면 스킵 (재기동 시 중복 방지)
            if (concertRepository.count() > 0) {
                return;
            }

            // 콘서트 1개
            Concert concert = concertRepository.save(new Concert(
                    "10cm 콘서트", "10cm",
                    "어쿠스틱 듀오 10cm의 단독 콘서트",
                    "올림픽공원 체조경기장",
                    "https://example.com/poster.jpg",
                    "https://example.com/thumb.jpg",
                    LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2),
                    2, ConcertStatus.OPEN
            ));

            // 스케줄 2개 (날짜 다름)
            Schedule schedule1 = scheduleRepository.save(new Schedule(
                    concert.getId(), LocalDate.of(2026, 8, 1), LocalTime.of(19, 0), 6, 6
            ));
            Schedule schedule2 = scheduleRepository.save(new Schedule(
                    concert.getId(), LocalDate.of(2026, 8, 2), LocalTime.of(19, 0), 6, 6
            ));

            // 각 스케줄당 좌석 6개 (VIP 2 + R 2 + S 2)
            seedSeats(seatRepository, schedule1.getId());
            seedSeats(seatRepository, schedule2.getId());

            // Redis 잔여 좌석 카운터 초기화 (api ↔ queue-worker 매진 통신 매개)
            soldOutService.initRemainingSeats(schedule1.getId(), schedule1.getTotalSeats());
            soldOutService.initRemainingSeats(schedule2.getId(), schedule2.getTotalSeats());
        };
    }

    private void seedSeats(SeatRepository seatRepository, Long scheduleId) {
        seatRepository.save(new Seat(scheduleId, "A-1", "VIP", 121000));
        seatRepository.save(new Seat(scheduleId, "A-2", "VIP", 121000));
        seatRepository.save(new Seat(scheduleId, "B-1", "R", 99000));
        seatRepository.save(new Seat(scheduleId, "B-2", "R", 99000));
        seatRepository.save(new Seat(scheduleId, "C-1", "S", 77000));
        seatRepository.save(new Seat(scheduleId, "C-2", "S", 77000));
    }
}
