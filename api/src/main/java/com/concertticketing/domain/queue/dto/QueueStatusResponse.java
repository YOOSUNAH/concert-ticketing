package com.concertticketing.domain.queue.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QueueStatusResponse {
    private int rank;
    private QueueStatus status;
    private String admissionToken;
}
