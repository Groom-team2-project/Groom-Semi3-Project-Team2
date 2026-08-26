package com.groom.moigo.domain.schedule.dto;

import com.groom.moigo.domain.place.dto.SchedulePlaceResponse;
import com.groom.moigo.domain.schedule.entity.ReservationStatus;
import com.groom.moigo.domain.schedule.entity.ScheduleEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ScheduleResponse {

    private Long scheduleId;
    private Long planId;
    private SchedulePlaceResponse place;
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

    public static ScheduleResponse from(ScheduleEntity schedule, SchedulePlaceResponse place) {
        return new ScheduleResponse(
                schedule.getScheduleId(),
                schedule.getPlanId(),
                place,
                schedule.getTitle(),
                schedule.getMemo(),
                schedule.getStartAt(),
                schedule.getEndAt(),
                schedule.getReservationStatus(),
                schedule.getSortOrder(),
                schedule.getKakaoRouteUrl(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt(),
                schedule.getDeletedAt()
        );
    }
}
