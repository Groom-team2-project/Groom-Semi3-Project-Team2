package com.groom.moigo.domain.schedule.dto;

import com.groom.moigo.domain.schedule.entity.ReservationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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

    @NotNull(message = "일정 순서는 필수입니다.")
    @PositiveOrZero(message = "일정 순서는 0 이상이어야 합니다.")
    private Integer sortOrder;

    // @Size(max = 500)
    // String = kakaoRouteUrl
}
