package com.groom.moigo.domain.schedule.dto;

import com.groom.moigo.domain.schedule.entity.ScheduleEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ScheduleDeleteResponse {

    private Long scheduleId;
    private LocalDateTime deletedAt;

    public static ScheduleDeleteResponse from(ScheduleEntity schedule) {
        return new ScheduleDeleteResponse(
                schedule.getScheduleId(),
                schedule.getDeletedAt()
        );
    }
}
