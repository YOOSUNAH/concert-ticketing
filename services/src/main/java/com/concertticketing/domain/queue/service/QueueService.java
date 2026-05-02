package com.concertticketing.domain.queue.service;

import com.concertticketing.domain.queue.dto.QueueEntryResult;
import com.concertticketing.domain.queue.dto.QueueStatusResult;
import com.concertticketing.domain.queue.repository.QueueRepository;

import java.util.List;

public class QueueService {

    private static final int ESTIMATED_WAIT_PER_PERSON_SECONDS = 5; // 1명당 예상 대기 5초
    private static final int HEARTBEAT_TTL_SECONDS = 30; // 30초간 폴링 없으면 이탈로 간주
    private static final int MAX_ACTIVE_COUNT = 2000; // 동시에 예매 가능한 최대 인원
    private static final int ACTIVE_EXPIRE_SECONDS = 600;  // ACTIVE 상태 10분 후 만료 (결제 안 하면 퇴장)"

    private final QueueRepository queueRepository;

    public QueueService(QueueRepository queueRepository) {
        this.queueRepository = queueRepository;
    }

    /**
     * (1) 대기열 입장
     * - WAITING Sorted Set에 추가 (score = 현재시각)
     * - heartbeat 설정
     * - 순번 조회 후 반환
     */
    public QueueEntryResult enterQueue(Long userId, Long scheduleId) {
        long now = System.currentTimeMillis();

        queueRepository.addToWaiting(scheduleId, userId, now);
        queueRepository.refreshHeartbeat(scheduleId, userId, HEARTBEAT_TTL_SECONDS);

        Long rank = queueRepository.getWaitingRank(scheduleId, userId);
        long displayRank = (rank != null) ? rank + 1 : 1;
        int waitSeconds = (int) displayRank * ESTIMATED_WAIT_PER_PERSON_SECONDS;

        return new QueueEntryResult(
                scheduleId + ":" + userId,
                displayRank,
                waitSeconds
        );
    }

    /**
     * (2) 대기 순번 조회 (폴링)
     * - heartbeat TTL 갱신
     * - ACTIVE이면 admissionToken 반환
     * - WAITING이면 현재 순번 + 예상 대기시간 반환
     */
    public QueueStatusResult getQueueStatus(Long userId, Long scheduleId) {
        // heartbeat 갱신 (살아있다는 신호)
        queueRepository.refreshHeartbeat(scheduleId, userId, HEARTBEAT_TTL_SECONDS);

        // ACTIVE인지 확인
        if (queueRepository.isActive(scheduleId, userId)) {
            String admissionToken = scheduleId + ":" + userId + ":admitted";
            return new QueueStatusResult(0, "ADMITTED", admissionToken);
        }

        // WAITING 순번 조회
        Long rank = queueRepository.getWaitingRank(scheduleId, userId);
        if (rank == null) {
            throw new IllegalArgumentException("대기열에 존재하지 않는 사용자입니다.");
        }

        long displayRank = rank + 1;
        return new QueueStatusResult(displayRank, "WAITING", null);
    }

    /**
     * (3) 스케줄러: WAITING → ACTIVE 전환 (3초마다 실행)
     * 1. 만료된 ACTIVE 정리
     * 2. 빈자리 계산
     * 3. WAITING 앞쪽에서 빈자리만큼 꺼냄 (heartbeat 확인)
     * 4. ACTIVE로 등록 + WAITING에서 제거
     */
    public void processQueue(Long scheduleId) {
        long now = System.currentTimeMillis();

        // 1. 만료된 ACTIVE 정리
        queueRepository.removeExpiredActive(scheduleId, now);

        // 2. 빈자리 계산
        long currentActive = queueRepository.getActiveCount(scheduleId);
        int availableSlots = (int) (MAX_ACTIVE_COUNT - currentActive);
        if (availableSlots <= 0) {
            return;
        }

        // 3. WAITING 앞쪽에서 후보 꺼내기 (여유있게 2배)
        List<Long> candidates = queueRepository.getTopWaiting(scheduleId, availableSlots * 2);

        int admitted = 0;
        for (Long candidateUserId : candidates) {
            if (admitted >= availableSlots) {
                break;
            }

            // heartbeat 확인 (이탈자 스킵)
            if (!queueRepository.isAlive(scheduleId, candidateUserId)) {
                queueRepository.removeFromWaiting(scheduleId, candidateUserId);
                continue;
            }

            // 4. ACTIVE로 등록 + WAITING에서 제거
            long expireAt = now + (ACTIVE_EXPIRE_SECONDS * 1000L);
            queueRepository.addToActive(scheduleId, candidateUserId, expireAt);
            queueRepository.removeFromWaiting(scheduleId, candidateUserId);
            admitted++;
        }
    }
}
