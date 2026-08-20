package com.groom.moigo.domain.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ScheduleOrderItemResponse {

    private Long scheduleId;
    private Integer sortOrder;
}
