package com.concertticketing.admin.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ScheduleCreateRequest(
        LocalDate date,
        LocalTime time,
        List<SeatGroupRequest> seatGroups
) {
}
