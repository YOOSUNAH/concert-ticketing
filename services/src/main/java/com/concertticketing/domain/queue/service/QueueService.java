package com.concertticketing.domain.queue.service;

import com.concertticketing.domain.queue.dto.QueueEntryResult;
import com.concertticketing.domain.queue.dto.QueueStatusResult;
import com.concertticketing.domain.queue.repository.QueueRepository;

import java.util.List;
import java.util.UUID;

public class QueueService {

    private static final int ESTIMATED_WAIT_PER_PERSON_SECONDS = 5;
    private static final int HEARTBEAT_TTL_SECONDS = 30;
    private static final int MAX_ACTIVE_COUNT = 2000;
    private static final int ACTIVE_EXPIRE_SECONDS = 600;
    private static final int TOKEN_TTL_SECONDS = 7200; // 2시간

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

        String queueToken = UUID.randomUUID().toString();
        queueRepository.saveTokenMapping(queueToken, scheduleId, userId, TOKEN_TTL_SECONDS);

        Long rank = queueRepository.getWaitingRank(scheduleId, userId);
        long displayRank = (rank != null) ? rank + 1 : 1;
        int waitSeconds = (int) displayRank * ESTIMATED_WAIT_PER_PERSON_SECONDS;

        return new QueueEntryResult(queueToken, displayRank, waitSeconds);
    }

    /**
     * (2) 대기 순번 조회 (폴링)
     * - heartbeat TTL 갱신
     * - ACTIVE이면 admissionToken 반환
     * - WAITING이면 현재 순번 + 예상 대기시간 반환
     */
    public QueueStatusResult getQueueStatus(Long userId, String queueToken) {
        Long scheduleId = resolveScheduleId(queueToken);

        queueRepository.refreshHeartbeat(scheduleId, userId, HEARTBEAT_TTL_SECONDS);

        if (queueRepository.isActive(scheduleId, userId)) {
            return new QueueStatusResult(0, "ADMITTED", queueToken);
        }

        Long rank = queueRepository.getWaitingRank(scheduleId, userId);
        if (rank == null) {
            throw new IllegalArgumentException("대기열에 존재하지 않는 사용자입니다.");
        }

        return new QueueStatusResult(rank + 1, "WAITING", null);
    }

    /**
     * admissionToken 검증
     * - 형식 일치 + 서버 측 ACTIVE 상태 동시 확인
     * - 클라이언트가 보낸 토큰 문자열만 믿지 않고, queueRepository로 실제 상태도 검증
     */
    public void validateAdmissionToken(Long userId, Long scheduleId, String admissionToken) {
        if (admissionToken == null || admissionToken.isBlank()) {
            throw new IllegalArgumentException("admissionToken이 없습니다.");
        }
        Long storedScheduleId = resolveScheduleId(admissionToken);
        if (!storedScheduleId.equals(scheduleId)) {
            throw new IllegalArgumentException("유효하지 않은 admissionToken입니다.");
        }
        if (!queueRepository.isActive(scheduleId, userId)) {
            throw new IllegalArgumentException("대기열 입장 권한이 만료되었거나 없습니다.");
        }
    }

    private Long resolveScheduleId(String queueToken) {
        String mapping = queueRepository.getTokenMapping(queueToken);
        if (mapping == null) {
            throw new IllegalArgumentException("유효하지 않은 queueToken입니다.");
        }
        return Long.parseLong(mapping.split(":")[0]);
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
