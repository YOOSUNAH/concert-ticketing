#!/usr/bin/env bash
# ──────────────────────────────────────────────
# 더미 유저 시드 (POST /users)
# 사용법: ./load-test/data/seed-users.sh [COUNT] [API_BASE]
# 예: ./load-test/data/seed-users.sh 1000
# ──────────────────────────────────────────────
set -euo pipefail

COUNT=${1:-200}
API_BASE=${2:-http://localhost:8080}

echo "=== Seeding $COUNT users to $API_BASE ==="

ok=0
fail=0

for i in $(seq 1 "$COUNT"); do
    status=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "$API_BASE/users" \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"loaduser-${i}@load.test\",\"password\":\"pw1234\",\"name\":\"loaduser-${i}\"}")

    if [ "$status" = "201" ] || [ "$status" = "409" ]; then
        ok=$((ok + 1))
    else
        fail=$((fail + 1))
    fi

    if [ $((i % 50)) -eq 0 ]; then
        echo "  $i / $COUNT  (ok=$ok, fail=$fail)"
    fi
done

echo "=== Done: ok=$ok, fail=$fail ==="
