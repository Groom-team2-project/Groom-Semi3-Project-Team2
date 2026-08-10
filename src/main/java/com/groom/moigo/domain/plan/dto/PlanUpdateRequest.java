package com.groom.moigo.domain.plan.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PlanUpdateRequest(
        @Size(max = 100) String title,
        @Size(max = 1000) String description,
        LocalDate startDate,
        LocalDate endDate,
        @Positive Integer recruitmentCount
) {
}