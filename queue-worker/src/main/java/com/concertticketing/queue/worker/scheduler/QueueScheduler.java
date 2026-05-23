package com.concertticketing.queue.worker.scheduler;

import com.concertticketing.domain.queue.repository.QueueRepository;
import com.concertticketing.domain.queue.service.QueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class QueueScheduler {

    private static final Logger log = LoggerFactory.getLogger(QueueScheduler.class);

    private final QueueService queueService;
    private final QueueRepository queueRepository;

    public QueueScheduler(QueueService queueService, QueueRepository queueRepository) {
        this.queueService = queueService;
        this.queueRepository = queueRepository;
    }

    /**
     * 대기열 처리 (3초마다)
     * - 매진 감지 → 큐 강제 종료 (WAITING 삭제 + closed 플래그)
     * - 평상시 → WAITING → ACTIVE 전환
     */
    @Scheduled(fixedDelay = 3000)
    public void processAllQueues() {
        Set<Long> activeSchedules = queueRepository.findActiveScheduleIds();
        for (Long scheduleId : activeSchedules) {
            if (queueService.isSoldOut(scheduleId)) {
                log.info("Schedule {} 매진 감지 → 큐 종료", scheduleId);
                queueService.closeSoldOutQueue(scheduleId);
                continue;
            }
            queueService.processQueue(scheduleId);
        }
    }

    /**
     * 고아 키 정리 (1시간마다)
     * - closed 플래그가 있고 waiting/active 모두 비어있는 scheduleId의 큐 관련 키 정리
     */
    @Scheduled(fixedDelay = 3600000)
    public void cleanupOrphanKeys() {
        Set<Long> activeSchedules = queueRepository.findActiveScheduleIds();
        for (Long scheduleId : activeSchedules) {
            String closedReason = queueRepository.getQueueClosedReason(scheduleId);
            if (closedReason != null && queueRepository.getActiveCount(scheduleId) == 0) {
                log.info("Schedule {} 고아 키 정리", scheduleId);
                queueRepository.deleteWaitingQueue(scheduleId);
            }
        }
    }
}
