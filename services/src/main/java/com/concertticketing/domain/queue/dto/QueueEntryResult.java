package com.concertticketing.domain.queue.dto;

public class QueueEntryResult {

    private final String queueToken;
    private final long rank;
    private final int estimatedWaitSeconds;

    public QueueEntryResult(String queueToken, long rank, int estimatedWaitSeconds) {
        this.queueToken = queueToken;
        this.rank = rank;
        this.estimatedWaitSeconds = estimatedWaitSeconds;
    }

    public String getQueueToken() {
        return queueToken;
    }

    public long getRank() {
        return rank;
    }

    public int getEstimatedWaitSeconds() {
        return estimatedWaitSeconds;
    }
}
