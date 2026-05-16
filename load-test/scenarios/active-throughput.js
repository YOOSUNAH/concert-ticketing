/**
 * ACTIVE throughput 테스트
 *
 * 목적: 본 api(8080)와 Postgres가 booking + payment 처리량을 견디는지 검증.
 *
 * 사전 시드 필수:
 *   ./load-test/data/seed-users.sh <TARGET_VUS>
 *
 * 설계:
 *   - setup(): 시드된 user 로그인 -> 큐 진입 -> ADMITTED 대기 -> admissionToken 캐시
 *     (큐를 우회해 ACTIVE 상태로 직접 진입)
 *   - default(): admissionToken으로 booking + payment 반복
 *
 * 시나리오 (api/application.yml의 queue.max-active-count 수정 후 재기동):
 *   30분 매진: MAX_ACTIVE=2000  -> TARGET_VUS=200~2000
 *   10분 매진: MAX_ACTIVE=6000  -> TARGET_VUS=2000~6000
 *   5분 매진:  MAX_ACTIVE=12000 -> TARGET_VUS=6000~12000
 *
 * 좌석은 6개뿐 -- 대다수가 booking 실패 응답 정상.
 *    booking API의 처리 시간 자체가 측정 대상.
 *
 * 환경변수:
 *   TARGET_VUS  (기본 200)
 *   DURATION    (기본 30s)
 *   SCHEDULE_ID (기본 1)
 *   LOGIN_DELAY 매 login 사이 sleep (기본 0.01)
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const TARGET_VUS = Number(__ENV.TARGET_VUS || 200);
const DURATION = __ENV.DURATION || '30s';
const SCHEDULE_ID = Number(__ENV.SCHEDULE_ID || 1);
const LOGIN_DELAY = Number(__ENV.LOGIN_DELAY || 0.01);
const API_BASE = __ENV.API_BASE || 'http://localhost:8080';
const QUEUE_BASE = __ENV.QUEUE_BASE || 'http://localhost:8081';

const bookingSuccess = new Counter('booking_success_count');
const bookingFailed = new Counter('booking_failed_count');
const paymentSuccess = new Counter('payment_success_count');

export const options = {
    setupTimeout: '15m',
    stages: [
        { duration: '10s', target: Math.floor(TARGET_VUS * 0.5) },
        { duration: '15s', target: TARGET_VUS },
        { duration: DURATION, target: TARGET_VUS },
        { duration: '10s', target: 0 },
    ],
    thresholds: {
        'http_req_duration{name:booking}': ['p(95)<3000'],
        'http_req_duration{name:payment}': ['p(95)<3000'],
    },
};

function tryLogin(email) {
    const res = http.post(`${API_BASE}/auth/login`,
        JSON.stringify({ email, password: 'pw1234' }),
        {
            headers: { 'Content-Type': 'application/json', 'Connection': 'close' },
            timeout: '10s',
        }
    );
    const auth = res.headers['Authorization'];
    return (auth && auth.startsWith('Bearer ')) ? auth : null;
}

function getAdmissionToken(auth) {
    const enterRes = http.post(`${QUEUE_BASE}/queue/enter`,
        JSON.stringify({ scheduleId: SCHEDULE_ID }),
        { headers: { 'Authorization': auth, 'Content-Type': 'application/json' } }
    );
    if (enterRes.status !== 200) return null;
    const queueToken = enterRes.json('queueToken');

    // queue-worker 스케줄러 3초 주기 x 최대 5번 폴링
    for (let j = 0; j < 5; j++) {
        sleep(3);
        const statusRes = http.get(
            `${QUEUE_BASE}/queue/status?queueToken=${queueToken}`,
            { headers: { 'Authorization': auth } }
        );
        if (statusRes.status === 200 && statusRes.json('status') === 'ADMITTED') {
            return statusRes.json('admissionToken');
        }
    }
    return null;
}

export function setup() {
    console.log(`Preparing ${TARGET_VUS} users -> login -> queue -> ADMITTED...`);
    const users = [];
    let withToken = 0;

    for (let i = 1; i <= TARGET_VUS; i++) {
        const email = `loaduser-${i}@load.test`;

        let auth = tryLogin(email);
        if (!auth) {
            sleep(0.5);
            auth = tryLogin(email);
        }
        if (!auth) {
            users.push(null);
            continue;
        }

        const admissionToken = getAdmissionToken(auth);
        users.push(admissionToken ? { auth, admissionToken } : null);
        if (admissionToken) withToken++;

        sleep(LOGIN_DELAY);
        if (i % 25 === 0) {
            console.log(`  ${i} / ${TARGET_VUS}  (ACTIVE: ${withToken})`);
        }
    }

    console.log(`Setup done. ACTIVE users: ${withToken} / ${TARGET_VUS}`);
    if (withToken === 0) {
        throw new Error('No ACTIVE users. Check queue-worker / seed-users.');
    }
    return { users };
}

export default function (data) {
    const user = data.users[__VU - 1];
    if (!user || !user.admissionToken) {
        sleep(60);
        return;
    }

    // 좌석 1~6 분산 시도. 동시 다수가 같은 좌석 노리는 매진 시나리오 자연 재현
    const seatId = (__VU % 6) + 1;

    const bookingRes = http.post(`${API_BASE}/bookings`,
        JSON.stringify({
            scheduleId: SCHEDULE_ID,
            seatIds: [seatId],
            admissionToken: user.admissionToken
        }),
        {
            headers: { 'Authorization': user.auth, 'Content-Type': 'application/json' },
            tags: { name: 'booking' }
        }
    );

    if (bookingRes.status === 201) {
        bookingSuccess.add(1);
        const bookingId = bookingRes.json('bookingId');
        const amount = bookingRes.json('totalAmount');

        const paymentRes = http.post(`${API_BASE}/payments/confirm`,
            JSON.stringify({
                bookingId,
                paymentKey: 'k6-active-key',
                orderId: `order-${bookingId}-${__VU}-${__ITER}`,
                amount,
                pointUsed: 0,
                paymentMethod: 'CARD'
            }),
            {
                headers: { 'Authorization': user.auth, 'Content-Type': 'application/json' },
                tags: { name: 'payment' }
            }
        );
        check(paymentRes, { 'payment 200': (r) => r.status === 200 });
        if (paymentRes.status === 200) paymentSuccess.add(1);
    } else {
        bookingFailed.add(1);
    }
}
