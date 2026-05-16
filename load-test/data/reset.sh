#!/usr/bin/env bash
# ──────────────────────────────────────────────
# 부하 테스트 환경 초기화
# 사용법:
#   ./load-test/data/reset.sh --redis    # Redis 큐 데이터만 초기화
#   ./load-test/data/reset.sh --seats    # 좌석만 AVAILABLE로 리셋
#   ./load-test/data/reset.sh --hard     # 전체 재시작 + 시드
# ──────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

reset_redis() {
    echo "=== Redis FLUSHDB ==="
    docker exec concert-redis redis-cli FLUSHDB
}

reset_seats() {
    echo "=== Seats → AVAILABLE ==="
    docker exec -i concert-postgres psql -U concert -d concert <<'SQL'
UPDATE seats SET status = 'AVAILABLE' WHERE schedule_id = 1;
DELETE FROM payments;
DELETE FROM bookings;
SELECT id, seat_number, status FROM seats WHERE schedule_id = 1;
SQL
}

reset_mongo() {
    echo "=== MongoDB re-seed ==="
    docker exec -i concert-mongo mongosh concert < "$SCRIPT_DIR/seed-mongo.js"
}

hard_reset() {
    echo "=== Hard reset: 전체 재시작 ==="
    reset_redis
    reset_seats
    reset_mongo
    echo "=== Hard reset done ==="
}

case "${1:---hard}" in
    --redis) reset_redis ;;
    --seats) reset_seats ;;
    --mongo) reset_mongo ;;
    --hard)  hard_reset ;;
    *)
        echo "Usage: $0 [--redis|--seats|--mongo|--hard]"
        exit 1
        ;;
esac
