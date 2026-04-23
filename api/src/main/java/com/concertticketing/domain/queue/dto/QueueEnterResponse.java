package com.concertticketing.domain.queue.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QueueEnterResponse {
    private String queueToken;
    private int rank;
    private int estimatedWaitSeconds;
}
