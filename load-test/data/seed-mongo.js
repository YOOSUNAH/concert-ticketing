/**
 * MongoDB 시드 — 콘서트 1건 + 스케줄 1건
 *
 * 사용법 (Docker 컨테이너):
 *   docker exec -i concert-mongo mongosh concert < load-test/data/seed-mongo.js
 *
 * 또는 docker-compose.yml 에서 mongo init 볼륨으로 자동 실행.
 */

db.concerts.deleteMany({});

db.concerts.insertOne({
    _id: NumberLong(1),
    title: "Load Test Concert",
    artist: "K6 Artist",
    description: "부하 테스트용 콘서트",
    venue: "테스트 공연장",
    posterUrl: "https://example.com/poster.jpg",
    thumbnailUrl: "https://example.com/thumb.jpg",
    startDate: ISODate("2026-06-01T00:00:00Z"),
    endDate: ISODate("2026-06-30T00:00:00Z"),
    maxTicketsPerPerson: 4,
    status: "OPEN",
    schedules: [
        {
            _id: NumberLong(1),
            concertId: NumberLong(1),
            date: ISODate("2026-06-15T00:00:00Z"),
            time: ISODate("1970-01-01T19:00:00Z"),
            totalSeats: 6,
            remainingSeats: 6
        }
    ],
    _class: "com.concertticketing.domain.concert.entity.Concert"
});

print("=== MongoDB seed done: 1 concert, 1 schedule ===");
