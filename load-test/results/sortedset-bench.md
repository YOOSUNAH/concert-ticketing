# Redis Sorted Set 자료구조 검증 결과

- 측정 일시: 2026-05-18
- 환경: Mac mini 로컬, Docker Compose (`concert-redis`만 기동)
- 도구: `redis-cli --pipe` (시드), `redis-benchmark` (측정)
- 키: `queue:waiting:1`
- 멤버: `userId` 문자열 / score: 입장 시각(ms)
- 측정 파라미터: `BENCH_N=20000`, `BENCH_C=50`

---

## 1. 측정한 연산과 그 의미

| 연산 | 호출 주체 | 빈도 |
|---|---|---|
| `ZRANK queue:waiting:1 <userId>` | queue-api 폴링 | 유저당 5초마다 |
| `ZRANGE 0 99` | QueueScheduler | 3초마다, admit 후보 추출 |
| `ZRANGE 0 3999` | QueueScheduler 풀배치 | 3초마다, max-active 한 번에 |
| `ZCARD` | 큐 길이 조회 | baseline (O(1)) |
| `ZADD <ts> <userId>` | queue-api 진입 | 진입 1회 |
| `ZREM <userId>` | QueueScheduler admit 직후 | admit 1회 |

---

## 2. 결과: 멤버 수 별 p50

| N (멤버 수) | 시드 시간 | ZRANK | ZRANGE 0-99 | ZCARD | ZRANGE 0-3999 | ZADD | ZREM |
|---|---|---|---|---|---|---|---|
| 10,000 | < 1s | 0.095ms | 0.183ms | 0.095ms | 2.919ms | 0.103ms | 0.095ms |
| 100,000 | 1s | 0.095ms | 0.183ms | 0.087ms | 2.895ms | 0.095ms | 0.095ms |
| **300,000** | **1s** | **0.095ms** | **0.183ms** | **0.087ms** | **2.871ms** | **0.143ms** | **0.087ms** |
| 500,000 | 1s | 0.095ms | 0.183ms | 0.087ms | 2.887ms | 0.111ms | 0.095ms |
| 1,000,000 | 2s | 0.095ms | 0.183ms | 0.087ms | 2.879ms | 0.119ms | 0.095ms |

## 3. 결과: 멤버 수 별 처리량 (RPS)

| N | ZRANK | ZRANGE 0-99 | ZCARD | ZRANGE 0-3999 | ZADD | ZREM |
|---|---|---|---|---|---|---|
| 10K | 266,667 | 139,860 | 289,855 | 5,879 | 259,740 | 285,714 |
| 100K | 285,714 | 138,889 | 303,030 | 5,968 | 303,030 | 294,118 |
| **300K** | **281,690** | **141,844** | **294,118** | **6,037** | **289,855** | **298,507** |
| 500K | 289,855 | 140,845 | 307,692 | 6,055 | 298,507 | 294,118 |
| 1M | 277,778 | 139,860 | 303,030 | 5,968 | 298,507 | 285,714 |

---

## 4. 해석

### 4-1. "30만 명이 Sorted Set에 들어갈 수 있는가" → YES

- 30만 멤버 주입 1초.
- 100만 멤버 주입 2초.
- 멤버 추가/삭제/순위조회 모두 sub-millisecond.

### 4-2. O(log N)이 실측에서 보이지 않는 이유

- 이론적으로 N이 100배(1만→100만) 늘면 log N은 약 1.67배 느려져야 함.
- 실측 p50은 0.087~0.095ms 사이에서 평탄. **측정 노이즈가 log N 증가폭보다 큼.**
- 즉, **30만~100만 규모에서 자료구조는 사실상 한계가 아님.** 응답시간이 일정.

### 4-3. 주의할 단 하나의 포인트: `ZRANGE 0 M-1`은 M에 비례

| 반환 크기 | p50 | RPS |
|---|---|---|
| 100개 (ZRANGE 0 99) | 0.183ms | ~140,000 |
| 4,000개 (ZRANGE 0 3999) | ~2.9ms | ~6,000 |

- O(log N + M)에서 N은 무시되고 **M(반환 크기)이 지배적**.
- 현재 스케줄러는 `getTopWaiting(scheduleId, availableSlots * 2)`이고 `max-active=2000` 기준 최대 4000개 한 번에 가져옴.
- **시사점**: 한 번에 너무 크게 꺼내면 Redis 단일 스레드를 ms 단위로 점유 → 다른 요청 지연. 100~500 단위로 페치하는 것이 안전 마진.

### 4-4. 실제 큐 시스템 처리 한계는 자료구조보다 다른 데 있음

- ZRANK 28만 RPS, ZADD 30만 RPS → Redis 단일 노드 명령 처리 한계는 ~30만 RPS 수준.
- 30만 명이 동시에 큐에 들어와도 Redis 자료구조는 **사실상 즉시 처리 가능** (이론상 1초).
- 진짜 병목은:
  1. queue-api Tomcat thread pool (기본 200) → HTTP 진입 수용
  2. JWT 발급/검증 CPU
  3. 네트워크 round-trip
- 즉 "Redis Sorted Set이 30만 명 못 견디는 것 아닌가" 라는 우려는 **자료구조 측면에서는 해소됨**. 한계가 있다면 그 위 레이어임.

---

## 5. 멘토 질문에 대한 답

> Q. 정말 sorted set에 수십만 명이 들어갈 수 있는가?

**A. 들어갑니다. 30만 ~ 100만 멤버 상태에서도 핵심 연산이 모두 sub-millisecond 이내에 처리되는 것을 실측으로 확인했습니다.**

- 30만 멤버: ZRANK 0.095ms, ZADD 0.143ms, 스케줄러 admit(ZRANGE 0 99) 0.183ms
- 100만 멤버에서도 동일한 응답시간 유지
- O(log N) 이론값이 측정 노이즈에 묻힐 정도로 평탄
- 단, 한 번에 수천 개를 반환하는 ZRANGE 0 3999는 ~2.9ms 소요 → 배치 사이즈를 100~500 단위로 제한하는 것이 안전

---

## 6. 재현 방법

```bash
# 1. Redis만 띄움 (다른 컨테이너 불필요)
docker compose up -d redis

# 2. queue-worker 등 mutator 중지 (떠 있다면)
docker stop concert-queue-worker concert-queue-api 2>/dev/null

# 3. N명 주입 (예: 30만)
./load-test/data/seed-redis-queue.sh 300000 1

# 4. 벤치 실행
BENCH_N=20000 ./load-test/scenarios/redis-sortedset-bench.sh 1

# 5. 다음 N으로 재측정 시 키 비우기
docker exec concert-redis redis-cli DEL queue:waiting:1
```
