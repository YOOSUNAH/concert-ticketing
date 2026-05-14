package com.concertticketing.queue.worker.scheduler;

import com.concertticketing.domain.queue.repository.QueueRepository;
import com.concertticketing.domain.queue.service.QueueService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class QueueScheduler {

    private final QueueService queueService;
    private final QueueRepository queueRepository;

    public QueueScheduler(QueueService queueService, QueueRepository queueRepository) {
        this.queueService = queueService;
        this.queueRepository = queueRepository;
    }

    /**
     * 대기열 처리 (3초마다)
     * - WAITING 키가 존재하는 모든 schedule에 대해 WAITING → ACTIVE 전환 시도
     */
    @Scheduled(fixedDelay = 3000)
    public void processAllQueues() {
        Set<Long> activeSchedules = queueRepository.findActiveScheduleIds();
        activeSchedules.forEach(queueService::processQueue);
    }
}
