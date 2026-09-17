package com.groom.moigo.domain.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ScheduleOrderResponse {

    private Long planId;
    private List<ScheduleOrderItemResponse> schedules;
}
