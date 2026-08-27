package com.groom.moigo.domain.schedule.dto;

import com.groom.moigo.domain.place.dto.SchedulePlaceResponse;
import com.groom.moigo.domain.schedule.entity.ReservationStatus;
import com.groom.moigo.domain.schedule.entity.ScheduleEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ScheduleSummaryResponse {

    private Long scheduleId;
    private SchedulePlaceResponse place;
    private String title;
    private Integer sortOrder;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private ReservationStatus reservationStatus;

    public static ScheduleSummaryResponse from(ScheduleEntity schedule, SchedulePlaceResponse place) {
        return new ScheduleSummaryResponse(
                schedule.getScheduleId(),
                place,
                schedule.getTitle(),
                schedule.getSortOrder(),
                schedule.getStartAt(),
                schedule.getEndAt(),
                schedule.getReservationStatus()
        );
    }
}
