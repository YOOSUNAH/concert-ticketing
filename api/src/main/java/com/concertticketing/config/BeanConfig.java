package com.concertticketing.config;

import com.concertticketing.domain.auth.jwt.JwtTokenProvider;
import com.concertticketing.domain.auth.service.AuthService;
import com.concertticketing.domain.booking.repository.BookingRepository;
import com.concertticketing.domain.booking.service.BookingService;
import com.concertticketing.domain.concert.repository.ConcertRepository;
import com.concertticketing.domain.concert.service.ConcertService;
import com.concertticketing.domain.payment.gateway.PaymentGateway;
import com.concertticketing.domain.payment.repository.PaymentRepository;
import com.concertticketing.domain.payment.service.PaymentService;
import com.concertticketing.domain.queue.config.QueueProperties;
import com.concertticketing.domain.queue.repository.QueueRepository;
import com.concertticketing.domain.queue.service.QueueService;
import com.concertticketing.domain.schedule.repository.ScheduleRepository;
import com.concertticketing.domain.seat.repository.SeatRepository;
import com.concertticketing.domain.seat.service.SeatService;
import com.concertticketing.domain.soldout.SoldOutService;
import com.concertticketing.domain.user.repository.UserRepository;
import com.concertticketing.domain.user.service.UserService;
import com.concertticketing.infra.payment.FakePaymentGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BeanConfig {

    // === Infra ===

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtTokenProvider jwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.validity-seconds}") long validitySeconds
    ) {
        return new JwtTokenProvider(secret, validitySeconds);
    }

    @Bean
    public PaymentGateway paymentGateway() {
        return new FakePaymentGateway();
    }

    // === Services (services 모듈은 순수 자바라 여기서 수동 등록) ===

    @Bean
    public UserService userService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return new UserService(userRepository, passwordEncoder);
    }

    @Bean
    public AuthService authService(UserRepository userRepository,
                                   PasswordEncoder passwordEncoder,
                                   JwtTokenProvider jwtTokenProvider) {
        return new AuthService(userRepository, passwordEncoder, jwtTokenProvider);
    }

    @Bean
    public ConcertService concertService(ConcertRepository concertRepository,
                                         ScheduleRepository scheduleRepository) {
        return new ConcertService(concertRepository, scheduleRepository);
    }

    @Bean
    public SeatService seatService(SeatRepository seatRepository) {
        return new SeatService(seatRepository);
    }

    @Bean
    public QueueProperties queueProperties(
            @Value("${queue.heartbeat-ttl-seconds}") int heartbeatTtlSeconds,
            @Value("${queue.max-active-count}") int maxActiveCount,
            @Value("${queue.active-expire-seconds}") int activeExpireSeconds,
            @Value("${queue.token-ttl-seconds}") int tokenTtlSeconds
    ) {
        return new QueueProperties(heartbeatTtlSeconds, maxActiveCount,
                activeExpireSeconds, tokenTtlSeconds);
    }

    @Bean
    public QueueService queueService(QueueRepository queueRepository, QueueProperties queueProperties) {
        return new QueueService(queueRepository, queueProperties);
    }

    @Bean
    public SoldOutService soldOutService(QueueRepository queueRepository) {
        return new SoldOutService(queueRepository);
    }

    @Bean
    public PaymentService paymentService(PaymentRepository paymentRepository,
                                         BookingRepository bookingRepository,
                                         UserRepository userRepository,
                                         PaymentGateway paymentGateway) {
        return new PaymentService(paymentRepository, bookingRepository, userRepository, paymentGateway);
    }

    @Bean
    public BookingService bookingService(BookingRepository bookingRepository,
                                         SeatService seatService,
                                         ConcertService concertService,
                                         PaymentService paymentService,
                                         QueueService queueService,
                                         SoldOutService soldOutService) {
        return new BookingService(bookingRepository, seatService, concertService,
                paymentService, queueService, soldOutService);
    }
}
