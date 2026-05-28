package com.concertticketing.domain.queue.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisQueueRepositoryTest {

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> valueOps;

    RedisQueueRepository sut;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOps);
        sut = new RedisQueueRepository(redis);
    }

    // ===== initRemainingSeats: 동적 TTL 검증 =====

    @Test
    void 잔여좌석_초기화_시_공연_다음날_자정까지의_TTL이_설정된다() {
        // given: 내일 공연
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // when
        sut.initRemainingSeats(1L, 100, tomorrow);

        // then: 값 저장
        verify(valueOps).set("schedule:1:remaining-seats", "100");

        // then: TTL 설정 (공연 다음날 자정까지)
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(redis).expire(eq("schedule:1:remaining-seats"), ttlCaptor.capture());

        Duration ttl = ttlCaptor.getValue();
        // 내일 공연 → 모레 자정까지 = 약 24~48시간 사이
        assertThat(ttl.toHours()).isBetween(24L, 48L);
    }

    @Test
    void 잔여좌석_초기화_시_오늘_공연이면_내일_자정까지_TTL() {
        // given: 오늘 공연
        LocalDate today = LocalDate.now();

        // when
        sut.initRemainingSeats(2L, 50, today);

        // then
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(redis).expire(eq("schedule:2:remaining-seats"), ttlCaptor.capture());

        Duration ttl = ttlCaptor.getValue();
        // 오늘 공연 → 내일 자정까지 = 0~24시간 사이
        assertThat(ttl.toHours()).isBetween(0L, 24L);
        assertThat(ttl.isNegative()).isFalse();
    }

    @Test
    void 잔여좌석_초기화_시_이미_지난_공연이면_TTL_설정_안함() {
        // given: 어제 공연 (plusDays(1).atStartOfDay() < now → isNegative)
        LocalDate yesterday = LocalDate.now().minusDays(2);

        // when
        sut.initRemainingSeats(3L, 30, yesterday);

        // then: 값은 저장되지만 expire는 호출되지 않음
        verify(valueOps).set("schedule:3:remaining-seats", "30");
        verify(redis, never()).expire(any(String.class), any(Duration.class));
    }

    // ===== markSoldOut: sold_out 키에 remaining-seats와 동일한 TTL 복사 =====

    @Test
    void 매진_마킹_시_잔여좌석_키의_TTL을_sold_out_키에_복사한다() {
        // given
        when(redis.getExpire("schedule:1:remaining-seats")).thenReturn(3600L);

        // when
        sut.markSoldOut(1L);

        // then: sold_out 플래그 설정
        verify(valueOps).set("schedule:1:sold_out", "1");

        // then: remaining-seats의 TTL을 sold_out에 복사
        verify(redis).expire("schedule:1:sold_out", Duration.ofSeconds(3600));
    }

    @Test
    void 매진_마킹_시_잔여좌석_키에_TTL이_없으면_sold_out에도_TTL_안_붙임() {
        // given: TTL이 -1 (만료 없음)
        when(redis.getExpire("schedule:1:remaining-seats")).thenReturn(-1L);

        // when
        sut.markSoldOut(1L);

        // then: sold_out은 설정하되 expire는 호출 안 됨
        verify(valueOps).set("schedule:1:sold_out", "1");
        verify(redis, never()).expire(eq("schedule:1:sold_out"), any(Duration.class));
    }
}
