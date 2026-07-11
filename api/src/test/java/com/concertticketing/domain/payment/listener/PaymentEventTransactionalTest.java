package com.concertticketing.domain.payment.listener;

import com.concertticketing.domain.payment.event.PaymentConfirmedEvent;
import com.concertticketing.infra.payment.PaymentConfirmedEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * EDA 핵심 계약(AFTER_COMMIT) 슬라이스 통합테스트.
 *
 * <p>실제 트랜잭션 경계가 필요하므로 H2 인메모리 + DataSourceTransactionManager로
 * 진짜 commit/rollback을 일으킨다. (Postgres/Docker 불필요 → 로컬에서 실행됨)
 *
 * <p>의도적으로 {@code @EnableAsync}를 켜지 않아 리스너가 동기로 실행되게 한다.
 * → AFTER_COMMIT의 "발화/미발화"를 결정적으로 단정하기 위함. (@Async 스레드 분리는 별도 관심사)
 */
@SpringJUnitConfig
class PaymentEventTransactionalTest {

    @Configuration
    @EnableTransactionManagement
    static class TestConfig {

        @Bean
        DataSource dataSource() {
            // 테이블도 필요 없다 — 트랜잭션 commit/rollback 동기화만 일으키면 된다.
            return new DriverManagerDataSource("jdbc:h2:mem:eda-aftercommit;DB_CLOSE_DELAY=-1", "sa", "");
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        PaymentConfirmedEventProducer paymentConfirmedEventProducer() {
            return mock(PaymentConfirmedEventProducer.class);
        }

        @Bean
        PaymentEventListener paymentEventListener(PaymentConfirmedEventProducer producer) {
            return new PaymentEventListener(producer);
        }

        @Bean
        TxPublisher txPublisher(ApplicationEventPublisher publisher) {
            return new TxPublisher(publisher);
        }
    }

    /** 트랜잭션 안에서 결제 완료 이벤트를 발행하는 테스트 헬퍼. */
    static class TxPublisher {
        private final ApplicationEventPublisher publisher;

        TxPublisher(ApplicationEventPublisher publisher) {
            this.publisher = publisher;
        }

        @Transactional
        public void publishWithinTx(boolean rollback) {
            publisher.publishEvent(new PaymentConfirmedEvent("evt-1", 999L, 1L, 237000, 5000));
            if (rollback) {
                throw new RuntimeException("강제 롤백");
            }
        }
    }

    @Autowired
    TxPublisher txPublisher;

    @Autowired
    PaymentConfirmedEventProducer producer;

    @BeforeEach
    void resetMock() {
        reset(producer); // 공유 컨텍스트라 테스트 간 호출 횟수 초기화
    }

    @Test
    @DisplayName("트랜잭션이 커밋되면 AFTER_COMMIT 리스너가 발화해 Kafka로 발행된다")
    void commit_firesListener() {
        txPublisher.publishWithinTx(false);
        verify(producer).publish(new PaymentConfirmedEvent("evt-1", 999L, 1L, 237000, 5000));
    }

    @Test
    @DisplayName("트랜잭션이 롤백되면 AFTER_COMMIT 리스너가 발화하지 않는다 (유령 알림 차단)")
    void rollback_doesNotFireListener() {
        assertThrows(RuntimeException.class, () -> txPublisher.publishWithinTx(true));
        verify(producer, never()).publish(any());
    }
}
