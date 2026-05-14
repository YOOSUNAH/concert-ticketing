package com.concertticketing.queue.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.lang.Nullable;

@Getter
@AllArgsConstructor
public class QueueStatusResponse {
    private int rank;
    private QueueStatus status;
    @Nullable
    private String admissionToken;
}
