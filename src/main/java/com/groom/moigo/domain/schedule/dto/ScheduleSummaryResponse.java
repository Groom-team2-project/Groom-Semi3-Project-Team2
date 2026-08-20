package com.groom.moigo.domain.schedule.dto;

import com.groom.moigo.domain.place.dto.PlaceResponse;
import com.groom.moigo.domain.schedule.entity.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ScheduleSummaryResponse {

    private Long scheduleId;
    private PlaceResponse place;
    private String title;
    private Integer sortOrder;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private ReservationStatus reservationStatus;
}
