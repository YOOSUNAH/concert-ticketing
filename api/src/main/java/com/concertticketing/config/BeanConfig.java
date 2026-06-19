package com.concertticketing.config;

import com.concertticketing.domain.auth.jwt.JwtTokenProvider;
import com.concertticketing.domain.auth.service.AuthService;
import com.concertticketing.domain.booking.repository.BookingRepository;
import com.concertticketing.domain.booking.service.BookingFacade;
import com.concertticketing.domain.booking.service.BookingService;
import com.concertticketing.domain.concert.repository.ConcertRefRepository;
import com.concertticketing.domain.concert.service.ConcertRefService;
import com.concertticketing.domain.payment.gateway.PaymentGateway;
import com.concertticketing.domain.payment.repository.PaymentRepository;
import com.concertticketing.domain.payment.service.PaymentService;
import com.concertticketing.domain.queue.config.QueueProperties;
import com.concertticketing.domain.queue.repository.QueueRepository;
import com.concertticketing.domain.queue.service.QueueService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.concertticketing.domain.schedule.repository.ScheduleRefRepository;
import com.concertticketing.domain.seat.repository.SeatRepository;
import com.concertticketing.domain.seat.service.SeatService;
import com.concertticketing.domain.soldout.SoldOutService;
import com.concertticketing.domain.user.repository.UserRepository;
import com.concertticketing.domain.user.service.UserService;
import com.concertticketing.infra.payment.FakePaymentGateway;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(QueueProperties.class)
public class BeanConfig {

    // === Infra ===

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public Cache<String, JwtTokenProvider.CachedToken> jwtTokenCache(
            @Value("${jwt.validity-seconds}") long validitySeconds
    ) {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofSeconds(validitySeconds))
                .build();
    }

    @Bean
    public JwtTokenProvider jwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.validity-seconds}") long validitySeconds,
            Cache<String, JwtTokenProvider.CachedToken> jwtTokenCache
    ) {
        return new JwtTokenProvider(secret, validitySeconds, jwtTokenCache);
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
    public ConcertRefService concertRefService(ConcertRefRepository concertRefRepository,
                                               ScheduleRefRepository scheduleRefRepository) {
        return new ConcertRefService(concertRefRepository, scheduleRefRepository);
    }

    @Bean
    public SeatService seatService(SeatRepository seatRepository) {
        return new SeatService(seatRepository);
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
    public BookingService bookingService(BookingRepository bookingRepository) {
        return new BookingService(bookingRepository);
    }

    @Bean
    public BookingFacade bookingFacade(BookingService bookingService,
                                       QueueService queueService,
                                       SeatService seatService,
                                       ConcertRefService concertRefService,
                                       PaymentService paymentService,
                                       SoldOutService soldOutService) {
        return new BookingFacade(bookingService, queueService, seatService,
                concertRefService, paymentService, soldOutService);
    }
}
