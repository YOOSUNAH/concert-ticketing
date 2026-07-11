# 콘서트 티켓팅 부하 테스트 보고서

- 테스트 일시: 2026-05-16
- 테스트 환경: Mac mini 로컬, Docker Compose (단일 인스턴스)
- 테스트 도구: k6 (Grafana)

---

## 1. 시스템 구성

| 컨테이너 | 포트 | 역할 | 의존 인프라 |
|-----------|------|------|-------------|
| api | 8080 | 예매/결제/좌석/사용자 | Postgres, Redis, MongoDB |
| concert-api | 8082 | 콘서트 조회 + Caffeine 캐시 | MongoDB |
| queue-api | 8081 | 큐 진입/폴링 | Redis |
| queue-worker | - | 대기열 스케줄러 (WAITING -> ADMITTED) | Redis |
| postgres | 5432 | 사용자/예매/결제/좌석 저장소 | - |
| redis | 6379 | 대기열/분산 락/하트비트 | - |
| mongo | 27017 | 콘서트/스케줄 문서 저장소 | - |

### 테스트 데이터

- 콘서트 1건, 스케줄 1건, 좌석 6개 (VIP 2, R 2, S 2)
- 좌석이 6개로 한정되어 대다수 예매는 매진 실패 응답 (의도된 설계)

---

## 2. 시나리오 개요

### 시나리오 1: waiting-capacity.js

| 항목 | 내용 |
|------|------|
| 질문 | 많은 사람이 큐에 들어올 때 견디는가? |
| 대상 | queue-api + Redis |
| 흐름 | setup: 시드 유저 로그인 -> default: 큐 진입 + 5초 간격 폴링 |
| 측정 | enter/status p95, admitted_count |

### 시나리오 2: active-throughput.js

| 항목 | 내용 |
|------|------|
| 질문 | 활성 사용자가 예매/결제할 때 견디는가? |
| 대상 | api + Postgres |
| 흐름 | setup: 로그인 -> 큐 통과 -> admissionToken 캐시 / default: booking + payment 반복 |
| 측정 | booking/payment p95, 성공/실패 카운트 |

### 시나리오 3: full-funnel.js

| 항목 | 내용 |
|------|------|
| 질문 | 실제 사용자 흐름 전체가 깨지지 않는가? |
| 대상 | 전체 통합 (concert-api -> queue-api -> api) |
| 흐름 | 콘서트 조회 -> 큐 진입 -> ADMITTED 폴링 -> 좌석 조회 -> 예매 -> 결제 |
| 측정 | step별 p95, funnel_completed, funnel_drop_at_step |

---

## 3. 테스트 결과

### 3-1. waiting-capacity (1000 VU / 1분)

```
결과: PASS
```

| 지표 | 값 | 기준 | 판정 |
|------|-----|------|------|
| http_req_failed | 0.05% | < 5% | PASS |
| enter p95 | 4.26ms | < 2000ms | PASS |
| status p95 | 5.07ms | < 1000ms | PASS |
| admitted_count | 18,571 | - | - |
| http_reqs 총합 | 38,158 (154 RPS) | - | - |

**해석**: 1000명 동시 큐 진입 + 5초 폴링 부하를 ms 단위로 안정 처리.
queue-api + Redis 조합이 단일 인스턴스에서도 충분한 성능.

---

### 3-2. active-throughput (200 VU / 30초)

```
결과: PASS
```

| 지표 | 값 | 기준 | 판정 |
|------|-----|------|------|
| booking p95 | 42.26ms | < 3000ms | PASS |
| booking avg | 19.09ms | - | - |
| booking_success | 600건 | - | - |
| booking_failed | 507,904건 | - | 정상 (좌석 6개) |
| http_reqs 총합 | 508,504 (736 RPS) | - | - |

해석: 200명이 동시에 예매를 시도해도 api + Postgres가 p95 42ms로 안정 응답.
실패 응답 대부분은 좌석 매진(6개 한정)에 의한 비즈니스 로직 실패로, 시스템 장애가 아님.

---

### 3-3. full-funnel (100 VU / 30초)

```
결과: PASS
```

| 지표 | 값 | 기준 | 판정 |
|------|-----|------|------|
| concert_list p95 | 4.39ms | < 500ms | PASS |
| http_req avg | 2.47ms | - | - |
| http_reqs 총합 | 878,022 (13,373 RPS) | - | - |
| funnel_completed | 6건 | - | - |
| payment 성공률 | 100% (6/6) | - | - |
| funnel_total_duration avg | 3,042ms | - | 큐 폴링 3초 포함 |

**해석**: 7단계 종단 흐름에서 특정 step에 병목 없이 전 구간 ms 단위 응답.
funnel_completed가 6건인 것은 좌석 6개 + 큐 ADMITTED 대기 구조상 정상.
결제까지 도달한 6건은 전부 성공 (100%).

---

## 4. 시나리오 비교

| 항목 | waiting-capacity | active-throughput | full-funnel |
|------|------------------|-------------------|-------------|
| 대상 | queue-api + Redis | api + Postgres | 전체 통합 |
| VU | 1,000 | 200 | 100 |
| 핵심 지표 | enter p95: 4.26ms | booking p95: 42.26ms | concert_list p95: 4.39ms |
| 처리량 | 154 RPS | 736 RPS | 13,373 RPS |
| 결과 | PASS | PASS | PASS |

---

## 5. 발견 및 해결한 이슈

### MongoDB 컨테이너 연결 실패

- **증상**: api / concert-api 컨테이너가 MongoDB에 연결 실패 (`Connection refused: localhost:27017`)
- **원인**: Spring Boot 4.0에서 MongoDB 속성 prefix가 변경됨
  - `spring.data.mongodb.*` (deprecated, error level)
  - `spring.mongodb.*` (신규)
- **해결**: application.yml과 docker-compose.yml의 속성명을 `spring.mongodb.uri`로 수정, 환경변수를 `SPRING_MONGODB_URI`로 변경

### MongoDB 시드 데이터 역직렬화 오류

- **증상**: `ConverterNotFoundException: No converter found capable of converting from type [java.lang.String] to type [java.time.LocalTime]`
- **원인**: seed-mongo.js에서 날짜/시간을 문자열(`"2026-06-15"`, `"19:00"`)로 저장. Spring Data MongoDB는 `LocalDate`/`LocalTime`을 `ISODate`로 변환하므로, 읽을 때 문자열 -> LocalTime 컨버터가 없음
- **해결**: `ISODate("2026-06-15T00:00:00Z")`, `ISODate("1970-01-01T19:00:00Z")` 형식으로 수정

---

## 6. 부하 테스트 실행 가이드

### 사전 준비

```bash
brew install k6
open -a Docker
docker compose up -d --build
```

### 시드 데이터 투입

```bash
docker exec -i concert-mongo mongosh concert < load-test/data/seed-mongo.js
docker exec -i concert-postgres psql -U concert -d concert < load-test/data/seed-seats.sql
./load-test/data/seed-users.sh 1000
```

### 실행

```bash
# 스모크 (동작 확인)
TARGET_VUS=10 DURATION=30s k6 run load-test/scenarios/waiting-capacity.js

# 베이스라인
TARGET_VUS=1000 DURATION=1m k6 run load-test/scenarios/waiting-capacity.js

# 시나리오 간 초기화
./load-test/data/reset.sh --hard
```

### 환경변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| TARGET_VUS | 동시 가상 사용자 수 | 시나리오별 상이 |
| DURATION | 유지 시간 | 30s ~ 1m |
| SCHEDULE_ID | 테스트 대상 스케줄 ID | 1 |
| LOGIN_DELAY | setup 로그인 간 sleep (초) | 0.01 |

---

## 7. 결론

Mac mini 로컬 Docker Compose 환경에서 단일 인스턴스 기준,
콘서트 티켓팅 시스템의 3개 핵심 경로가 모두 안정적으로 동작함을 확인.

- **큐 시스템**: 1,000 동시 사용자를 ms 단위로 처리
- **예매/결제**: 200 동시 사용자 기준 p95 42ms
- **종단 흐름**: 100 동시 사용자 기준 13,373 RPS, 병목 구간 없음

향후 VU를 더 올리거나(2000+), 실제 클라우드 환경에서 다중 인스턴스 구성 시
추가 부하 테스트를 통해 스케일링 기준점을 확보할 수 있음.
