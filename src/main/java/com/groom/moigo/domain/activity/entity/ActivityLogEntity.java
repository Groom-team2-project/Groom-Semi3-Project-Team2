package com.groom.moigo.domain.activity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    // 투표 자동 마감, 일정 상태 변경, 만료 초대 취소처럼 시스템이 자동 처리한 활동 로그는 수행한 사용자가 없으므로 null을 허용합니다.
    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ActivityActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private ActivityTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "summary", nullable = false, length = 300)
    private String summary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static ActivityLogEntity create(
            Long planId,
            Long userId,
            ActivityActionType actionType,
            ActivityTargetType targetType,
            Long targetId,
            String summary
    ) {
        ActivityLogEntity log = new ActivityLogEntity();
        log.planId = planId;
        log.userId = userId;
        log.actionType = actionType;
        log.targetType = targetType;
        log.targetId = targetId;
        log.summary = summary;
        log.createdAt = LocalDateTime.now();
        return log;
    }
}
