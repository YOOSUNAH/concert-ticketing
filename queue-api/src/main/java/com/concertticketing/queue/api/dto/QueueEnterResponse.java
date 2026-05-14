package com.concertticketing.queue.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QueueEnterResponse {
    private String queueToken;
    private int rank;
    private int estimatedWaitSeconds;
}
