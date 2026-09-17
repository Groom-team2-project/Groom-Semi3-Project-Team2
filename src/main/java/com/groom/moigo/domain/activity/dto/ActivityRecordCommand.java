package com.groom.moigo.domain.activity.dto;

import com.groom.moigo.domain.activity.entity.ActivityActionType;
import com.groom.moigo.domain.activity.entity.ActivityTargetType;

public record ActivityRecordCommand(
        Long planId,
        Long userId,
        ActivityActionType actionType,
        ActivityTargetType targetType,
        Long targetId,
        String summary
) {
}
