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
import java.time.temporal.ChronoUnit;

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
        // created_at 컬럼은 DATETIME(6)이라 마이크로초까지만 저장됨.
        // 리눅스에서는 now()가 나노초까지 나오므로, 그대로 두면 메모리 값과 DB 저장값이 달라져 커서 페이지네이션의 동등 비교가 어긋나게 됨.
        log.createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        return log;
    }
}
