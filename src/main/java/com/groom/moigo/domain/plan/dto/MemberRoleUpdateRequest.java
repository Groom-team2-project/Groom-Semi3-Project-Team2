package com.groom.moigo.domain.plan.dto;

import com.groom.moigo.domain.plan.entity.MemberRole;
import jakarta.validation.constraints.NotNull;

public record MemberRoleUpdateRequest(
        @NotNull MemberRole role
) {
}