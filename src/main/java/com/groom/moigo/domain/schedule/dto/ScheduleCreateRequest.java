package com.groom.moigo.domain.schedule.dto;

import com.groom.moigo.domain.schedule.entity.ReservationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ScheduleCreateRequest {

    private Long placeId;

    @NotBlank @Size(max = 200)
    private String title;

    @Size(max = 1000)
    private String memo;

    @NotNull
    private LocalDateTime startAt;

    private LocalDateTime endAt;

    @NotNull
    private ReservationStatus reservationStatus;

}
