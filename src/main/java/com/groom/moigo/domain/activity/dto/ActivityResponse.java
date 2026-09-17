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
        /** 대상이 삭제되어 이동할 수 없는 활동인지. 현재는 댓글 대상만 판별하고 그 외에는 항상 false. */
        boolean targetDeleted,
        String summary,
        Long userId,
        String nickname,
        String profileImage,
        LocalDateTime createdAt
) {
    public static ActivityResponse from(
            ActivityLogEntity log,
            UserEntity user,
            String planTitle,
            Long scheduleId,
            boolean targetDeleted
    ) {
        return new ActivityResponse(
                log.getLogId(), log.getPlanId(), planTitle, log.getActionType().name(), log.getTargetType().name(), log.getTargetId(),
                scheduleId, targetDeleted, log.getSummary(), log.getUserId(),
                user == null ? "시스템" : user.getNickname(),
                user == null ? null : user.getProfileImage(), log.getCreatedAt()
        );
    }
}
