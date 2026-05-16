/**
 * WAITING capacity 테스트
 *
 * 목적: queue-api(8081) + Redis가 대량 큐 진입 + 폴링을 견디는지 검증.
 *
 * 사전 시드 필수:
 *   ./load-test/data/seed-users.sh <TARGET_VUS>
 *
 * 환경변수:
 *   TARGET_VUS  (기본 1000)
 *   DURATION    (기본 1m)
 *   POLL_TIMES  (기본 5)
 *   LOGIN_DELAY (기본 0.01)
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const TARGET_VUS = Number(__ENV.TARGET_VUS || 1000);
const DURATION = __ENV.DURATION || '1m';
const POLL_TIMES = Number(__ENV.POLL_TIMES || 5);
const LOGIN_DELAY = Number(__ENV.LOGIN_DELAY || 0.01);
const API_BASE = __ENV.API_BASE || 'http://localhost:8080';
const QUEUE_BASE = __ENV.QUEUE_BASE || 'http://localhost:8081';

const admittedCount = new Counter('admitted_count');

export const options = {
    setupTimeout: '10m',
    stages: [
        { duration: '10s', target: Math.floor(TARGET_VUS * 0.5) },
        { duration: '15s', target: TARGET_VUS },
        { duration: DURATION, target: TARGET_VUS },
        { duration: '10s', target: 0 },
    ],
    thresholds: {
        'http_req_failed': ['rate<0.05'],
        'http_req_duration{name:enter}': ['p(95)<2000'],
        'http_req_duration{name:status}': ['p(95)<1000'],
    },
};

export function setup() {
    console.log(`Preparing ${TARGET_VUS} tokens...`);
    const tokens = [];
    let valid = 0;
    let retried = 0;

    for (let i = 1; i <= TARGET_VUS; i++) {
        const email = `loaduser-${i}@load.test`;
        let res = http.post(`${API_BASE}/auth/login`,
            JSON.stringify({ email, password: 'pw1234' }),
            {
                headers: { 'Content-Type': 'application/json', 'Connection': 'close' },
                timeout: '10s',
            }
        );

        let auth = res.headers['Authorization'];
        if (!auth || !auth.startsWith('Bearer ')) {
            sleep(0.5);
            retried++;
            res = http.post(`${API_BASE}/auth/login`,
                JSON.stringify({ email, password: 'pw1234' }),
                {
                    headers: { 'Content-Type': 'application/json', 'Connection': 'close' },
                    timeout: '10s',
                }
            );
            auth = res.headers['Authorization'];
        }

        tokens.push(auth && auth.startsWith('Bearer ') ? auth : null);
        if (auth) valid++;
        sleep(LOGIN_DELAY);

        if (i % 100 === 0) {
            console.log(`  ${i} / ${TARGET_VUS}  (valid=${valid}, retried=${retried})`);
        }
    }

    console.log(`Setup done. Valid: ${valid} / ${TARGET_VUS}, retried: ${retried}`);
    if (valid === 0) throw new Error('No valid tokens. Check seed-users / api.');
    return { tokens };
}

export default function (data) {
    const auth = data.tokens[__VU - 1];
    if (!auth) return;

    // 큐 진입
    const enterRes = http.post(`${QUEUE_BASE}/queue/enter`,
        JSON.stringify({ scheduleId: 1 }),
        {
            headers: { 'Authorization': auth, 'Content-Type': 'application/json' },
            tags: { name: 'enter' },
        }
    );
    if (enterRes.status !== 200) return;

    const queueToken = enterRes.json('queueToken');

    // 폴링
    for (let i = 0; i < POLL_TIMES; i++) {
        sleep(5);
        const statusRes = http.get(
            `${QUEUE_BASE}/queue/status?queueToken=${queueToken}`,
            {
                headers: { 'Authorization': auth },
                tags: { name: 'status' },
            }
        );
        if (statusRes.status === 200 && statusRes.json('status') === 'ADMITTED') {
            admittedCount.add(1);
            break;
        }
    }
}
