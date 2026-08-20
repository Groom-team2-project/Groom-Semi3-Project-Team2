package com.groom.moigo.domain.schedule.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

import java.util.List;

@Getter
public class ScheduleOrderRequest {

    @NotEmpty
    private List<Long> scheduleIds;
}
