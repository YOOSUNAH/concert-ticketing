#!/usr/bin/env bash
# ──────────────────────────────────────────────
# Redis Sorted Set 시드 — 대기열에 N명 주입
#
# 사용법: ./load-test/data/seed-redis-queue.sh [COUNT] [SCHEDULE_ID]
# 예:     ./load-test/data/seed-redis-queue.sh 300000 1
#
# queue:waiting:{scheduleId} 키에 userId를 score=입장시각(ms)으로 삽입.
# redis-cli --pipe 를 사용하여 대량 삽입 고속 처리.
# ──────────────────────────────────────────────
set -euo pipefail

COUNT=${1:-300000}
SCHEDULE_ID=${2:-1}
REDIS_HOST=${3:-localhost}
REDIS_PORT=${4:-6379}
KEY="queue:waiting:${SCHEDULE_ID}"

echo "=== Seeding ${COUNT} members into ${KEY} ==="

# 기존 키 삭제
docker exec concert-redis redis-cli DEL "$KEY" > /dev/null

# redis-cli --pipe 용 RESP 프로토콜 생성 + 파이프 전송
# score = base_timestamp + i (순서 보장)
BASE_TS=1700000000000

generate_commands() {
    for i in $(seq 1 "$COUNT"); do
        SCORE=$((BASE_TS + i))
        MEMBER="user-${i}"
        printf "*4\r\n\$4\r\nZADD\r\n\$${#KEY}\r\n${KEY}\r\n\$${#SCORE}\r\n${SCORE}\r\n\$${#MEMBER}\r\n${MEMBER}\r\n"
    done
}

START=$(date +%s)
generate_commands | docker exec -i concert-redis redis-cli --pipe 2>&1 | tail -1
END=$(date +%s)
ELAPSED=$((END - START))

# 확인
CARD=$(docker exec concert-redis redis-cli ZCARD "$KEY")
echo "=== Done: ${CARD} members in ${KEY} (${ELAPSED}s) ==="
