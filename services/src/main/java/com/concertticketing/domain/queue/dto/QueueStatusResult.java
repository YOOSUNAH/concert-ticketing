package com.concertticketing.domain.queue.dto;

public class QueueStatusResult {

    private final long rank;
    private final String status;
    private final String admissionToken;

    public QueueStatusResult(long rank, String status, String admissionToken) {
        this.rank = rank;
        this.status = status;
        this.admissionToken = admissionToken;
    }

    public long getRank() {
        return rank;
    }

    public String getStatus() {
        return status;
    }

    public String getAdmissionToken() {
        return admissionToken;
    }
}
