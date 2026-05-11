package com.concertticketing.domain.queue.scheduler;

import com.concertticketing.domain.queue.service.QueueService;
import com.concertticketing.domain.schedule.repository.SpringDataScheduleRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class QueueScheduler {

    private final QueueService queueService;
    private final SpringDataScheduleRepository scheduleRepository;

    public QueueScheduler(QueueService queueService,
                          SpringDataScheduleRepository scheduleRepository) {
        this.queueService = queueService;
        this.scheduleRepository = scheduleRepository;
    }

    /**
     * 대기열 처리 (3초마다)
     * - 모든 schedule에 대해 WAITING → ACTIVE 전환 시도
     */
    @Scheduled(fixedDelay = 3000)
    public void processAllQueues() {
        scheduleRepository.findAll()
                .forEach(s -> queueService.processQueue(s.getId()));
    }
}
