#!/bin/bash
#
# 시드 데이터 투입 스크립트
# Admin API를 통해 공연/스케줄/좌석/Redis 카운터를 일괄 생성합니다.
# dual-write(PostgreSQL + MongoDB + Redis)가 보장됩니다.
#
# 사용법:
#   ./scripts/seed-data.sh                  # 기본: localhost:8083
#   ./scripts/seed-data.sh http://host:port # Admin API 주소 지정
#

ADMIN_API="${1:-http://localhost:8083}"

echo "=== 시드 데이터 투입 시작 (Admin API: $ADMIN_API) ==="

# 콘서트 1: 10cm 콘서트
echo ""
echo "[1/2] 10cm 콘서트 생성 중..."
RESULT=$(curl -s -w "\n%{http_code}" -X POST "$ADMIN_API/admin/concerts" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "10cm 콘서트",
    "artist": "10cm",
    "description": "어쿠스틱 듀오 10cm의 단독 콘서트",
    "venue": "올림픽공원 체조경기장",
    "posterUrl": "https://example.com/10cm-poster.jpg",
    "thumbnailUrl": "https://example.com/10cm-thumb.jpg",
    "startDate": "2026-08-01",
    "endDate": "2026-08-02",
    "maxTicketsPerPerson": 2,
    "schedules": [
      {
        "date": "2026-08-01",
        "time": "19:00",
        "seatGroups": [
          {"grade": "VIP", "seatPrefix": "VIP", "count": 2, "price": 150000},
          {"grade": "R", "seatPrefix": "R", "count": 2, "price": 120000},
          {"grade": "S", "seatPrefix": "S", "count": 2, "price": 100000}
        ]
      },
      {
        "date": "2026-08-02",
        "time": "19:00",
        "seatGroups": [
          {"grade": "VIP", "seatPrefix": "VIP", "count": 2, "price": 150000},
          {"grade": "R", "seatPrefix": "R", "count": 2, "price": 120000},
          {"grade": "S", "seatPrefix": "S", "count": 2, "price": 100000}
        ]
      }
    ]
  }')

HTTP_CODE=$(echo "$RESULT" | tail -1)
BODY=$(echo "$RESULT" | sed '$d')

if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
  echo "  -> 성공: $BODY"
else
  echo "  -> 실패 (HTTP $HTTP_CODE): $BODY"
fi

# 콘서트 2: IU 콘서트
echo ""
echo "[2/2] IU 콘서트 생성 중..."
RESULT=$(curl -s -w "\n%{http_code}" -X POST "$ADMIN_API/admin/concerts" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "IU 콘서트 - The Golden Hour",
    "artist": "IU",
    "description": "IU 전국 투어 콘서트",
    "venue": "KSPO DOME",
    "posterUrl": "https://example.com/iu-poster.jpg",
    "thumbnailUrl": "https://example.com/iu-thumb.jpg",
    "startDate": "2026-09-15",
    "endDate": "2026-09-16",
    "maxTicketsPerPerson": 4,
    "schedules": [
      {
        "date": "2026-09-15",
        "time": "18:00",
        "seatGroups": [
          {"grade": "VIP", "seatPrefix": "VIP", "count": 5, "price": 180000},
          {"grade": "R", "seatPrefix": "R", "count": 5, "price": 140000},
          {"grade": "S", "seatPrefix": "S", "count": 10, "price": 100000}
        ]
      },
      {
        "date": "2026-09-16",
        "time": "18:00",
        "seatGroups": [
          {"grade": "VIP", "seatPrefix": "VIP", "count": 5, "price": 180000},
          {"grade": "R", "seatPrefix": "R", "count": 5, "price": 140000},
          {"grade": "S", "seatPrefix": "S", "count": 10, "price": 100000}
        ]
      }
    ]
  }')

HTTP_CODE=$(echo "$RESULT" | tail -1)
BODY=$(echo "$RESULT" | sed '$d')

if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
  echo "  -> 성공: $BODY"
else
  echo "  -> 실패 (HTTP $HTTP_CODE): $BODY"
fi

echo ""
echo "=== 시드 데이터 투입 완료 ==="
