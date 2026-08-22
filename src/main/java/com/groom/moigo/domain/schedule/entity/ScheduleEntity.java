package com.groom.moigo.domain.schedule.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "schedules")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "place_id")
    private Long placeId;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "memo", length = 1000)
    private String memo;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_status", nullable = false)
    private ReservationStatus reservationStatus;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "kakao_route_url", length = 500)
    private String kakaoRouteUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private ScheduleEntity(
            Long planId,
            Long placeId,
            String title,
            String memo,
            LocalDateTime startAt,
            LocalDateTime endAt,
            ReservationStatus reservationStatus,
            Integer sortOrder
    ) {
        validateTimeRange(startAt, endAt);

        this.planId = planId;
        this.placeId = placeId;
        this.title = title;
        this.memo = memo;
        this.startAt = startAt;
        this.endAt = endAt;
        this.reservationStatus = reservationStatus;
        this.sortOrder = sortOrder;
    }

    public void update(
            Long placeId,
            String title,
            String memo,
            LocalDateTime startAt,
            LocalDateTime endAt,
            ReservationStatus reservationStatus,
            Boolean clearPlace,
            Boolean clearMemo,
            Boolean clearEndAt
    ) {
        Long nextPlaceId = Boolean.TRUE.equals(clearPlace) ? null : placeId != null ? placeId : this.placeId;
        String nextTitle = title != null ? title : this.title;
        String nextMemo = Boolean.TRUE.equals(clearMemo) ? null : memo != null ? memo : this.memo;
        LocalDateTime nextStartAt = startAt != null ? startAt : this.startAt;
        LocalDateTime nextEndAt = Boolean.TRUE.equals(clearEndAt) ? null : endAt != null ? endAt : this.endAt;
        ReservationStatus nextReservationStatus = reservationStatus != null ? reservationStatus : this.reservationStatus;

        validateTimeRange(nextStartAt, nextEndAt);

        this.placeId = nextPlaceId;
        this.title = nextTitle;
        this.memo = nextMemo;
        this.startAt = nextStartAt;
        this.endAt = nextEndAt;
        this.reservationStatus = nextReservationStatus;
    }

    public void reorder(
            Integer sortOrder
    ) {
        if (sortOrder == null || sortOrder < 0) {
            throw new IllegalArgumentException(
                    "순서는 0 이상"
            );
        }
        this.sortOrder = sortOrder;
    }

    public void softDelete(){
        if (this.deletedAt == null)
            this.deletedAt = LocalDateTime.now();
    }

    private static void validateTimeRange(LocalDateTime startAt, LocalDateTime endAt) {
        if (endAt != null && endAt.isBefore(startAt)) {
            throw new IllegalArgumentException(
                    "종료 시간은 시작 시간보다 빠를 수 없습니다."
            );
        }
    }

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        LocalDateTime now = LocalDateTime.now();
        this.updatedAt = now;
    }
}
