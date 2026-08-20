package com.groom.moigo.domain.schedule.dto;

import com.groom.moigo.domain.place.dto.PlaceResponse;
import com.groom.moigo.domain.schedule.entity.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ScheduleResponse {

    private Long scheduleId;
    private Long planId;
    private PlaceResponse place;
    private String title;
    private String memo;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private ReservationStatus reservationStatus;
    private Integer sortOrder;
    private String kakaoRouteUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
