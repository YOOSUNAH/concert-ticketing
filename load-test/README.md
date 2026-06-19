# 부하 테스트 (k6)

콘서트 티켓팅 시스템의 부하 한계 검증.

## 시나리오 3종

| 시나리오 | 대상 | 측정 |
|---|---|---|
| `scenarios/waiting-capacity.js` | queue-api(8081) + Redis | 동시 enter + 폴링 처리량, p95, Redis CPU/메모리 |
| `scenarios/active-throughput.js` | api(8080) + Postgres | MAX_ACTIVE별 booking/payment 처리량 |
| `scenarios/full-funnel.js` | 전체 (enter→ADMITTED→예매→결제) | 종단 사용자 흐름 안정성 |

## 사전 준비

### 1. k6 설치
```bash
brew install k6
```

### 2. 시스템 띄우기
```bash
docker compose up -d --build
```
- api: http://localhost:8080
- queue-api: http://localhost:8081
- concert-api: http://localhost:8082
- 시드 데이터: Concert 1개, Schedule 2개(id=1,2), 좌석 12석

### 3. 테스트 user 시드 (Full funnel만 필요)
```bash
./data/seed-users.sh 1000   # 1000명 더미 user 생성
```

## 실행 방법

### WAITING capacity 테스트
```bash
# 5만 동시 사용자 (로컬)
k6 run scenarios/waiting-capacity.js

# 환경변수로 부하 조절
TARGET_VUS=500000 DURATION=2m k6 run scenarios/waiting-capacity.js
```

### ACTIVE throughput 테스트
시나리오별로 application.yml의 `queue.max-active-count`를 미리 조정:
```yaml
# api/src/main/resources/application.yml
queue:
  max-active-count: 2000   # 30분 매진 시나리오
  # max-active-count: 6000  # 10분 매진
  # max-active-count: 12000 # 5분 매진
```
조정 후 재기동:
```bash
docker compose up -d --build api
k6 run scenarios/active-throughput.js
```

### Full funnel
```bash
./data/seed-users.sh 1000
k6 run scenarios/full-funnel.js
```

## 리셋 (시나리오 간)

```bash
# Redis 큐 상태만 비우기 (Sentinel 구성: master에 실행 → replica로 전파)
docker exec concert-redis-master redis-cli FLUSHALL

# 좌석/예매까지 깨끗하게
docker compose down -v && docker compose up -d --build
```

## 결과 보기

k6는 실행 종료 후 터미널에 요약 출력:
- `http_req_duration` — 응답 시간 (avg/min/med/max/p90/p95)
- `http_req_failed` — 에러율
- `iterations` — 총 시나리오 반복 횟수
- `vus` — 동시 가상 유저 수

추가 시각화 원하면 InfluxDB + Grafana 연동 (선택, 후속 작업).

## 권장 시나리오 진행 순서

```
1. 스모크 — VU 10, 30초 (스크립트 자체 동작 확인)
2. 베이스라인 — VU 1,000, 1분 (시스템 정상 응답 확인)
3. 본격 — VU 50,000, 2분 (실제 부하)
4. 한계 측정 — VU 점진 증가 (어디서 깨지는지 관찰)
```

각 단계 끝나면 Redis FLUSHALL로 정리 후 다음 단계.
