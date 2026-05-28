/**
 * Full Funnel 종단 시나리오
 *
 * 목적: 실제 사용자 흐름을 모사하여 통합 안정성 검증.
 *       단독 인스턴스 한계가 아닌, 흐름 중 어디서 병목이 터지는지 관찰.
 *
 * 설계:
 *   - setup(): VU 수만큼 user 미리 생성+로그인 (토큰 캐시)
 *   - default(): 토큰으로 콘서트 조회 -> 큐 진입 -> ADMITTED -> 좌석 -> 예매 -> 결제
 *               매 iteration마다 새 큐 진입 (서로 다른 사용자 시나리오 모사)
 *
 * 환경변수:
 *   TARGET_VUS  최대 동시 VU (기본 100 -- 좌석 6개라 매진 자연 발생)
 *   DURATION    유지 시간 (기본 30s)
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const TARGET_VUS = Number(__ENV.TARGET_VUS || 100);
const DURATION = __ENV.DURATION || '30s';
const API_BASE = __ENV.API_BASE || 'http://localhost:8080';
const QUEUE_BASE = __ENV.QUEUE_BASE || 'http://localhost:8081';
const CONCERT_BASE = __ENV.CONCERT_BASE || 'http://localhost:8082';

const funnelCompleted = new Counter('funnel_completed');
const funnelDropAtStep = new Counter('funnel_drop_at_step');
const funnelTotalDuration = new Trend('funnel_total_duration');

export const options = {
    setupTimeout: '10m',
    stages: [
        { duration: '10s', target: TARGET_VUS },
        { duration: DURATION, target: TARGET_VUS },
        { duration: '10s', target: 0 },
    ],
    thresholds: {
        'http_req_duration{step:concert_list}': ['p(95)<500'],
    },
};

export function setup() {
    console.log(`Preparing ${TARGET_VUS} users...`);
    const tokens = [];
    for (let i = 1; i <= TARGET_VUS; i++) {
        const email = `loadfunnel-${i}-${Date.now()}@load.test`;
        const password = 'pw1234';

        const signUpRes = http.post(`${API_BASE}/users`,
            JSON.stringify({ email, password, name: `funnel-${i}` }),
            { headers: { 'Content-Type': 'application/json' } }
        );
        if (signUpRes.status !== 201) { tokens.push(null); continue; }

        const loginRes = http.post(`${API_BASE}/auth/login`,
            JSON.stringify({ email, password }),
            { headers: { 'Content-Type': 'application/json' } }
        );
        const auth = loginRes.headers['Authorization'];
        tokens.push(auth && auth.startsWith('Bearer ') ? auth : null);
    }
    console.log(`Setup done. Valid tokens: ${tokens.filter(Boolean).length}`);
    return { tokens };
}

export default function (data) {
    const authHeader = data.tokens[__VU - 1];
    if (!authHeader) return;

    const startTs = Date.now();

    // === 1. 콘서트 목록/상세 (concert-api 캐시) ===
    const listRes = http.get(`${CONCERT_BASE}/concerts?page=0&size=10`,
        { tags: { step: 'concert_list' } });
    if (listRes.status !== 200) {
        funnelDropAtStep.add(1, { step: 'concert_list' }); return;
    }
    const detailRes = http.get(`${CONCERT_BASE}/concerts/1`,
        { tags: { step: 'concert_detail' } });
    if (detailRes.status !== 200) {
        funnelDropAtStep.add(1, { step: 'concert_detail' }); return;
    }

    // === 2. 큐 진입 ===
    const enterRes = http.post(`${QUEUE_BASE}/queue/enter`,
        JSON.stringify({ scheduleId: 1 }),
        {
            headers: { 'Authorization': authHeader, 'Content-Type': 'application/json' },
            tags: { step: 'enter' }
        }
    );
    if (enterRes.status !== 200) {
        funnelDropAtStep.add(1, { step: 'enter' }); return;
    }
    const queueToken = enterRes.json('queueToken');

    // === 3. ADMITTED 폴링 ===
    let admissionToken = null;
    for (let i = 0; i < 10; i++) {
        sleep(3);
        const statusRes = http.get(
            `${QUEUE_BASE}/queue/status?queueToken=${queueToken}`,
            { headers: { 'Authorization': authHeader }, tags: { step: 'poll' } }
        );
        if (statusRes.status === 200 && statusRes.json('status') === 'ADMITTED') {
            admissionToken = statusRes.json('admissionToken');
            break;
        }
    }
    if (!admissionToken) {
        funnelDropAtStep.add(1, { step: 'admission_timeout' }); return;
    }

    // === 4. 좌석 조회 ===
    const seatsRes = http.get(`${API_BASE}/schedules/1/seats`,
        { headers: { 'Authorization': authHeader }, tags: { step: 'seats' } });
    if (seatsRes.status !== 200) {
        funnelDropAtStep.add(1, { step: 'seats' }); return;
    }

    // === 5. 예매 ===
    const seatId = (__VU % 6) + 1;
    const bookingRes = http.post(`${API_BASE}/bookings`,
        JSON.stringify({ scheduleId: 1, seatIds: [seatId], admissionToken }),
        {
            headers: { 'Authorization': authHeader, 'Content-Type': 'application/json' },
            tags: { step: 'booking' }
        }
    );
    if (bookingRes.status !== 201) {
        funnelDropAtStep.add(1, { step: 'booking_failed' }); return;
    }
    const bookingId = bookingRes.json('bookingId');
    const totalAmount = bookingRes.json('totalAmount');

    // === 6. 결제 ===
    const paymentRes = http.post(`${API_BASE}/payments/confirm`,
        JSON.stringify({
            bookingId, paymentKey: 'k6-funnel-key',
            orderId: `order-${bookingId}`,
            amount: totalAmount, pointUsed: 0, paymentMethod: 'CARD'
        }),
        {
            headers: { 'Authorization': authHeader, 'Content-Type': 'application/json' },
            tags: { step: 'payment' }
        }
    );
    check(paymentRes, { 'payment 200': (r) => r.status === 200 });

    if (paymentRes.status === 200) {
        funnelCompleted.add(1);
        funnelTotalDuration.add(Date.now() - startTs);
    } else {
        funnelDropAtStep.add(1, { step: 'payment_failed' });
    }
}
