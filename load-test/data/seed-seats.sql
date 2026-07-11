-- 좌석 시드 — schedule_id=1 에 6개 좌석
-- 사용법: docker exec -i concert-postgres psql -U concert -d concert < load-test/data/seed-seats.sql

DELETE FROM seats WHERE schedule_id = 1;

INSERT INTO seats (schedule_id, seat_number, grade, price, status) VALUES
    (1, 'A1', 'VIP',     150000, 'AVAILABLE'),
    (1, 'A2', 'VIP',     150000, 'AVAILABLE'),
    (1, 'A3', 'R',       120000, 'AVAILABLE'),
    (1, 'A4', 'R',       120000, 'AVAILABLE'),
    (1, 'A5', 'S',       100000, 'AVAILABLE'),
    (1, 'A6', 'S',       100000, 'AVAILABLE');

SELECT id, seat_number, grade, price, status FROM seats WHERE schedule_id = 1;
