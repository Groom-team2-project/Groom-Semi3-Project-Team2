package com.groom.moigo.domain.plan.dto;

import com.groom.moigo.domain.plan.entity.MemberRole;
import com.groom.moigo.domain.plan.entity.PlanEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PlanResponse(
        Long planId,
        String title,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        Integer recruitmentCount,
        Long ownerId,
        MemberRole myRole,
        long memberCount,
        LocalDateTime createdAt
) {
    public static PlanResponse of(PlanEntity plan, MemberRole myRole, long memberCount) {
        return new PlanResponse(
                plan.getPlanId(),
                plan.getTitle(),
                plan.getDescription(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.getRecruitmentCount(),
                plan.getOwner().getUserId(),
                myRole,
                memberCount,
                plan.getCreatedAt()
        );
    }
}