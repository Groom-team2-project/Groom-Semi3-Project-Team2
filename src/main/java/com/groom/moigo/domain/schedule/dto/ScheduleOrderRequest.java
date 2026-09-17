package com.groom.moigo.domain.schedule.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

@Getter
public class ScheduleOrderRequest {

    @NotEmpty
    private List<@NotNull Long> scheduleIds;
}
