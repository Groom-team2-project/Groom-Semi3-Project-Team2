package com.groom.moigo.domain.plan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "plan_places",
        uniqueConstraints = @UniqueConstraint(name = "uk_plan_places_plan_place", columnNames = {"plan_id", "place_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanPlaceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long planPlaceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanEntity plan;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static PlanPlaceEntity create(PlanEntity plan, Long placeId) {
        PlanPlaceEntity planPlace = new PlanPlaceEntity();
        planPlace.plan = plan;
        planPlace.placeId = placeId;
        return planPlace;
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
