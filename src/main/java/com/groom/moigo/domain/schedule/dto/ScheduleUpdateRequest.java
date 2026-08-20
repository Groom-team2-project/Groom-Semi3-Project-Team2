package com.groom.moigo.domain.schedule.dto;

import com.groom.moigo.domain.schedule.entity.ReservationStatus;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ScheduleUpdateRequest {

    private Long placeId;

    @Size(max = 200)
    private String title;

    @Size(max = 1000)
    private String memo;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private ReservationStatus reservationStatus;

    // @Size(max = 500)
    // String = kakaoRouteUrl

    // Boolean clearPlace;
    // Boolean clearMemo;
}
