package com.groom.moigo.domain.activity.dto;

import com.groom.moigo.domain.activity.entity.ActivityLogEntity;
import com.groom.moigo.domain.user.entity.UserEntity;

import java.time.LocalDateTime;

public record ActivityResponse(
        Long logId,
        Long planId,
        String planTitle,
        String actionType,
        String targetType,
        Long targetId,
        Long scheduleId,
        String summary,
        Long userId,
        String nickname,
        String profileImage,
        LocalDateTime createdAt
) {
    public static ActivityResponse from(ActivityLogEntity log, UserEntity user, String planTitle, Long scheduleId) {
        return new ActivityResponse(
                log.getLogId(), log.getPlanId(), planTitle, log.getActionType().name(), log.getTargetType().name(), log.getTargetId(),
                scheduleId, log.getSummary(), log.getUserId(),
                user == null ? "시스템" : user.getNickname(),
                user == null ? null : user.getProfileImage(), log.getCreatedAt()
        );
    }
}
