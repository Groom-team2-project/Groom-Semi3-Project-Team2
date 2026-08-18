package com.groom.moigo.domain.activity.service;

import com.groom.moigo.domain.activity.dto.ActivityRecordCommand;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ActivityLogServiceImpl implements ActivityLogService {
    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final PlanAccessService planAccessService;
    private final PlanRepository planRepository;

    private static final Set<ActivityActionType> SHARED_ACTIONS = Set.of(
            ActivityActionType.SCHEDULE_CREATED,
            ActivityActionType.SCHEDULE_UPDATED,
            ActivityActionType.SCHEDULE_DELETED,
            ActivityActionType.VOTE_CREATED,
            ActivityActionType.VOTE_CLOSED,
            ActivityActionType.MEMBER_JOINED,
            ActivityActionType.MEMBER_LEFT,
            ActivityActionType.MEMBER_ROLE_CHANGED,
            ActivityActionType.COMMENT_CREATED
    );

    @Override
    public void record(ActivityRecordCommand command) {
        ActivityLogEntity log = ActivityLogEntity.create(
                command.planId(),
                command.userId(),
                command.actionType(),
                command.targetType(),
                command.targetId(),
                command.summary()
        );

        activityLogRepository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityResponse> getActivities(Long planId, Long userId) {
        planAccessService.requireJoinedMember(planId, userId);
        List<ActivityLogEntity> logs = activityLogRepository.findByPlanIdOrderByCreatedAtDesc(planId)
                .stream().filter(log -> SHARED_ACTIONS.contains(log.getActionType())).toList();
        return toResponses(logs);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityResponse> getMyActivities(Long userId) {
        return toResponses(activityLogRepository.findByUserIdOrderByCreatedAtDesc(userId));
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
