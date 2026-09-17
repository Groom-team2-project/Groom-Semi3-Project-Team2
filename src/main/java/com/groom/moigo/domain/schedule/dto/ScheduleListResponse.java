package com.groom.moigo.domain.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ScheduleListResponse {

    private Long planId;
    private List<ScheduleSummaryResponse> schedules;
}
