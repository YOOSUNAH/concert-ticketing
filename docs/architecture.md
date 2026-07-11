# 콘서트 티켓팅 시스템 아키텍처

## 서버 구조

```
프론트 → api.example.com (단일 주소)
              │
         API Gateway (Nginx :80)
              │
    ┌─────────┼──────────┐
    ▼         ▼          ▼
/concerts/** /queue/**   /**
Concert API  Queue API  Main API
  :8082       :8081      :8080

Admin API :8083 ← 관리자 직접 접근 (Gateway 미경유)
```

- 프론트는 하나의 주소만 알면 됨
- Gateway가 URL 경로로 분배
- Rate Limit: 좌석 조회 3r/s, 공연 조회 5r/s, 큐 2r/s (IP당)
- 프록시 캐시: 좌석 조회 응답 3초 캐싱
- Admin API는 내부망 전용, Gateway를 경유하지 않음

---

## 서버별 상세

### api (port 8080) — 비즈니스 로직

예매, 결제, 좌석, 사용자, 인증을 담당하는 핵심 서버.

- **PostgreSQL (JPA)**
  - booking, payment, user — 읽기/쓰기
  - seat — 읽기/쓰기 (상태 변경: AVAILABLE ↔ SOLD)
  - concert_ref, schedule_ref — 읽기 전용 (admin-api가 쓰기 담당)

- **Redis (재고)**
  - `schedule:{scheduleId}:remaining-seats` — 잔여 좌석 카운터
  - `schedule:{scheduleId}:sold_out` — 매진 플래그 (TTL 없음, 취소 시 로직으로 삭제)

- 좌석 동시성 제어: PostgreSQL 조건부 UPDATE (`WHERE status = 'AVAILABLE'`)
- 공연 정보 조회는 concert-api가 분리 흡수 → 예매/결제에 집중

---

### concert-api (port 8082) — 공연/스케줄 조회 (읽기 전용)

- **MongoDB**
  - Concert 문서 (`concerts` 컬렉션)
  - Schedule은 별도 컬렉션이 아니라 Concert 문서 안에 임베디드 (`List<Schedule>`)

- **Redis (매진 상태 조회)**
  - `schedule:{scheduleId}:sold_out` — 매진 여부 조회 (응답에 soldOut 필드 포함)

- **Caffeine 캐시**
  - `concert-list` — 목록 조회 캐시 (1시간 TTL, max 1000)

- **엔드포인트** (GET만 존재, POST/PUT/DELETE 없음)
  - `GET /concerts?page=0&size=10` — 목록 조회
  - `GET /concerts/{concertId}` — 상세 + 스케줄 조회 (각 스케줄에 soldOut 포함)

---

### admin-api (port 8083) — 공연 데이터 쓰기 담당

두 DB에 걸친 데이터 일관성을 보장하는 유일한 쓰기 서버.

- **PostgreSQL + MongoDB (dual-write)**
  - 스케줄(MongoDB)과 좌석(PostgreSQL)이 서로를 참조하는 구조
  - 한 곳에서 양쪽을 동시에 써야 불일치 방지
  - `@Transactional` 내에서 PostgreSQL 먼저, MongoDB 후 — 실패 시 롤백

- **Redis**
  - `schedule:{scheduleId}:remaining-seats` — 공연 생성 시 잔여 좌석 초기화

- **엔드포인트**
  - `POST /admin/concerts` — 공연 생성 (양쪽 DB + 좌석 + Redis)
  - `PUT /admin/concerts/{concertId}` — 공연 정보 수정 (dual-write)
  - `GET /admin/concerts` — 공연 목록 조회
  - `POST /admin/concerts/{concertId}/schedules` — 스케줄 추가

---

### queue-api (port 8081) — 대기열 진입, 폴링, 상태 조회

- **Redis 키 구조**
  - `queue:waiting:{scheduleId}` — 대기 ZSet (score = 입장시각 ms, FIFO)
  - `queue:active:{scheduleId}` — 활성 ZSet (ADMITTED된 유저)
  - `queue:hb:{scheduleId}:{userId}` — 하트비트 (타임스탬프 기반 생존 확인)
  - `queue:token:{uuid}` — 큐 토큰 → scheduleId:userId 매핑
  - `queue:{scheduleId}:closed` — 매진 시 큐 종료 플래그 (폴링 응답용)

- **엔드포인트**
  - `POST /queue/enter` — 큐 진입 (queueToken + rank 반환)
  - `GET /queue/status?queueToken=...` — 상태 폴링 (WAITING/ADMITTED + admissionToken)

---

### queue-worker — 스케줄러 전용 (WAITING → ACTIVE 승격)

- **동작 방식**
  - `@Scheduled(fixedDelay = 3000)` — 3초마다 실행
  - WAITING ZSet에서 score 낮은 순(먼저 온 순)으로 후보 추출
  - 하트비트가 살아있는 유저만 ACTIVE ZSet으로 이동
  - `schedule:{scheduleId}:sold_out` 플래그 확인 → 매진 시 큐 종료

- **Redis** — queue-api와 동일한 키 구조 전부 사용

- **다중화**
  - replicas: 2로 다중화 (리더 선출 미적용, 두 워커가 독립 실행)

---

## DB 분리 전략

### 트랜잭션 데이터 → PostgreSQL (RDB)

| 데이터 | 테이블 | 특성 |
|--------|--------|------|
| 예매 | bookings | ACID 무결성 필수 |
| 결제 | payments | 결제 분쟁 가능성, 정합성 최우선 |
| 사용자 | users | 인증/포인트 관리 |
| 좌석 | seats | 상태 변경 (AVAILABLE/SOLD) |

ACID 보장이 필요한 데이터. 결제 분쟁 시 트랜잭션 로그가 근거가 됨.

### 정적 메타데이터 → MongoDB

| 데이터 | 컬렉션 | 특성 |
|--------|--------|------|
| 공연 정보 | concerts | 거의 변하지 않음, 읽기 위주 |
| 스케줄 | (concerts에 임베디드) | Concert와 항상 함께 조회 |

스키마 유연성 + 임베디드 모델로 조인 없는 단일 쿼리 조회.

### 이미지 데이터 → Object Storage

| 데이터 | 저장소 | 특성 |
|--------|--------|------|
| 공연 포스터, 아티스트 사진 | Cloudflare R2 / AWS S3 | 용량 큼, CDN으로 분산 전달 |

DB에는 URL만 저장 (`posterUrl`, `thumbnailUrl`).
CDN(Content Delivery Network)을 통해 사용자 가까운 엣지 서버에서 전달.

### 실시간 상태 데이터 → Redis

| 데이터 | 키 패턴 | 특성 |
|--------|---------|------|
| 잔여 좌석 수 | `schedule:{scheduleId}:remaining-seats` | 초당 수천 쓰기 |
| 매진 플래그 | `schedule:{scheduleId}:sold_out` | 로직 기반 삭제 |

### 큐 / 휘발성 데이터 → Redis

| 데이터 | 키 패턴 | 특성 |
|--------|---------|------|
| 대기열 | `queue:waiting:{scheduleId}` ZSet | 정렬/카운트가 빨라야 함 |
| 활성 유저 | `queue:active:{scheduleId}` ZSet | TTL 자동 만료 |
| 큐 토큰 | `queue:token:{uuid}` | 휘발성 |
| 하트비트 | `queue:hb:{scheduleId}:{userId}` | 타임스탬프 기반 생존 확인 |
| 큐 종료 | `queue:{scheduleId}:closed` | 매진 시 설정 |

### 인증 토큰 → JWT (Stateless)

| 데이터 | 저장소 | 특성 |
|--------|--------|------|
| Access Token | JWT (서명 기반) | Redis/DB 불필요 |

무상태 검증 가능. 만료 시간 내장. 토큰 블랙리스트/세션 스토어 없음.
`Authorization: Bearer {token}` 헤더로 전달, 서명 검증만으로 인증 완료.
