#!/usr/bin/env bash
# ──────────────────────────────────────────────
# Redis Sorted Set 벤치마크
#
# 사용법: ./load-test/scenarios/redis-sortedset-bench.sh [SCHEDULE_ID]
# 환경변수:
#   BENCH_N  요청 수 (기본 20000)
#   BENCH_C  동시 연결 수 (기본 50)
#
# 사전 조건: seed-redis-queue.sh 로 데이터 주입 완료
# ──────────────────────────────────────────────
set -euo pipefail

SCHEDULE_ID=${1:-1}
KEY="queue:waiting:${SCHEDULE_ID}"
N=${BENCH_N:-20000}
C=${BENCH_C:-50}

CARD=$(docker exec concert-redis redis-cli ZCARD "$KEY")
echo "============================================"
echo " Redis Sorted Set Benchmark"
echo " Key: ${KEY}  |  Members: ${CARD}"
echo " Requests: ${N}  |  Concurrency: ${C}"
echo "============================================"
echo ""

run_bench() {
    local label=$1
    shift
    echo "--- ${label} ---"
    docker exec concert-redis redis-benchmark -n "$N" -c "$C" -q "$@" 2>&1
    echo ""
}

# 1. ZCARD — O(1) baseline
run_bench "ZCARD (baseline O(1))" ZCARD "$KEY"

# 2. ZRANK — O(log N), 폴링 시 순위 조회
#    중간 위치 멤버로 테스트
MID_MEMBER="user-$((CARD / 2))"
run_bench "ZRANK (mid member: ${MID_MEMBER})" ZRANK "$KEY" "$MID_MEMBER"

# 3. ZRANGE 0 99 — 스케줄러 기본 배치 (100명)
run_bench "ZRANGE 0 99 (top 100)" ZRANGE "$KEY" 0 99

# 4. ZRANGE 0 3999 — 스케줄러 풀배치 (4000명, max-active=2000 기준)
run_bench "ZRANGE 0 3999 (top 4000)" ZRANGE "$KEY" 0 3999

# 5. ZADD — 큐 진입 (신규 멤버 추가)
#    테스트용 임시 멤버 사용, 매번 덮어쓰기
run_bench "ZADD (write)" ZADD "$KEY" 9999999999999 "bench-temp-user"

# 6. ZREM — admit 후 삭제
run_bench "ZREM (delete)" ZREM "$KEY" "bench-temp-user"

# 최종 멤버 수 확인
FINAL_CARD=$(docker exec concert-redis redis-cli ZCARD "$KEY")
echo "============================================"
echo " Final member count: ${FINAL_CARD}"
echo "============================================"
