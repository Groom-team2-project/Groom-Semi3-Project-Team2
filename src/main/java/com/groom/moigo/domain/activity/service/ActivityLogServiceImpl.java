package com.groom.moigo.domain.activity.service;

import com.groom.moigo.domain.activity.dto.ActivityRecordCommand;
import com.groom.moigo.domain.activity.dto.ActivityPageResponse;
import com.groom.moigo.domain.activity.dto.ActivityResponse;
import com.groom.moigo.domain.activity.entity.ActivityLogEntity;
import com.groom.moigo.domain.activity.entity.ActivityActionType;
import com.groom.moigo.domain.activity.repository.ActivityLogRepository;
import com.groom.moigo.domain.activity.entity.ActivityTargetType;
import com.groom.moigo.domain.comment.entity.CommentEntity;
import com.groom.moigo.domain.comment.repository.CommentRepository;
import com.groom.moigo.domain.user.entity.UserEntity;
import com.groom.moigo.domain.user.repository.UserRepository;
import com.groom.moigo.domain.plan.service.PlanAccessService;
import com.groom.moigo.domain.plan.entity.PlanEntity;
import com.groom.moigo.domain.plan.repository.PlanRepository;
import com.groom.moigo.global.error.BusinessException;
import com.groom.moigo.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ActivityLogServiceImpl implements ActivityLogService {
    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final PlanAccessService planAccessService;
    private final PlanRepository planRepository;
    private final PlatformTransactionManager transactionManager;

    private static final Set<ActivityActionType> SHARED_ACTIONS = Set.of(
            ActivityActionType.SCHEDULE_CREATED,
            ActivityActionType.SCHEDULE_UPDATED,
            ActivityActionType.SCHEDULE_DELETED,
            ActivityActionType.VOTE_CREATED,
            ActivityActionType.VOTE_UPDATED,
            ActivityActionType.VOTE_DELETED,
            ActivityActionType.VOTE_CLOSED,
            ActivityActionType.MEMBER_JOINED,
            ActivityActionType.MEMBER_LEFT,
            ActivityActionType.MEMBER_ROLE_CHANGED,
            ActivityActionType.COMMENT_CREATED
    );

    // 활동 기록 실패가 원래 도메인 작업에 영향을 주면 안 됨(정책서 5절 2항). @Transactional만으로는 커밋 시점
    // 예외를 못 잡으므로 TransactionTemplate으로 트랜잭션 제어를 직접 함.
    @Override
    public void record(ActivityRecordCommand command) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        try {
            transactionTemplate.executeWithoutResult(status -> {
                ActivityLogEntity activityLog = ActivityLogEntity.create(
                        command.planId(),
                        command.userId(),
                        command.actionType(),
                        command.targetType(),
                        command.targetId(),
                        command.summary()
                );

                activityLogRepository.save(activityLog);
            });
        } catch (RuntimeException e) {
            log.error(
                    "활동 기록 저장 실패: planId={}, userId={}, actionType={}",
                    command.planId(), command.userId(), command.actionType(), e
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityPageResponse getActivities(
            Long planId,
            Long userId,
            int size,
            LocalDateTime cursorCreatedAt,
            Long cursorLogId
    ) {
        planAccessService.requireJoinedMember(planId, userId);
        validateCursor(cursorCreatedAt, cursorLogId);
        List<ActivityLogEntity> logs = activityLogRepository.findSharedActivitiesByCursor(
                planId,
                List.copyOf(SHARED_ACTIONS),
                cursorCreatedAt,
                cursorLogId,
                PageRequest.of(0, size + 1)
        );
        return toPageResponse(logs, size);
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityPageResponse getMyActivities(
            Long userId,
            int size,
            LocalDateTime cursorCreatedAt,
            Long cursorLogId
    ) {
        validateCursor(cursorCreatedAt, cursorLogId);
        List<ActivityLogEntity> logs = activityLogRepository.findMyActivitiesByCursor(
                userId,
                cursorCreatedAt,
                cursorLogId,
                PageRequest.of(0, size + 1)
        );
        return toPageResponse(logs, size);
    }

    private void validateCursor(LocalDateTime cursorCreatedAt, Long cursorLogId) {
        if ((cursorCreatedAt == null) != (cursorLogId == null)) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "활동 기록 커서는 생성 시각과 로그 ID를 함께 전달해야 합니다."
            );
        }
    }

    private ActivityPageResponse toPageResponse(List<ActivityLogEntity> logs, int size) {
        boolean hasNext = logs.size() > size;
        List<ActivityLogEntity> pageLogs = hasNext ? logs.subList(0, size) : logs;
        List<ActivityResponse> activities = toResponses(pageLogs);
        ActivityLogEntity lastLog = hasNext ? pageLogs.getLast() : null;

        return new ActivityPageResponse(
                activities,
                lastLog == null ? null : lastLog.getCreatedAt(),
                lastLog == null ? null : lastLog.getLogId(),
                hasNext
        );
    }

    private List<ActivityResponse> toResponses(List<ActivityLogEntity> logs) {
        Map<Long, UserEntity> usersById = userRepository.findAllById(
                        logs.stream().map(ActivityLogEntity::getUserId).filter(java.util.Objects::nonNull).distinct().toList()
                ).stream()
                .collect(Collectors.toMap(UserEntity::getUserId, Function.identity()));
        Map<Long, Long> scheduleIdsByCommentId = commentRepository.findAllById(
                        logs.stream()
                                .filter(log -> log.getTargetType() == ActivityTargetType.COMMENT)
                                .map(ActivityLogEntity::getTargetId)
                                .toList()
                ).stream()
                .collect(Collectors.toMap(CommentEntity::getCommentId, CommentEntity::getScheduleId));
        Map<Long, String> planTitlesById = planRepository.findAllById(
                        logs.stream().map(ActivityLogEntity::getPlanId).distinct().toList()
                ).stream()
                .collect(Collectors.toMap(PlanEntity::getPlanId, PlanEntity::getTitle));

        return logs.stream()
                .map(log -> ActivityResponse.from(
                        log,
                        usersById.get(log.getUserId()),
                        planTitlesById.getOrDefault(log.getPlanId(), "삭제된 계획"),
                        log.getTargetType() == ActivityTargetType.COMMENT
                                ? scheduleIdsByCommentId.get(log.getTargetId())
                                : null
                ))
                .toList();
    }
}
