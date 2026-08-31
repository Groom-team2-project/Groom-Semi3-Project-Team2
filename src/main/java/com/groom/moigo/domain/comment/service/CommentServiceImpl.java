package com.groom.moigo.domain.comment.service;

import com.groom.moigo.domain.activity.dto.ActivityRecordCommand;
import com.groom.moigo.domain.activity.entity.ActivityActionType;
import com.groom.moigo.domain.activity.entity.ActivityTargetType;
import com.groom.moigo.domain.activity.service.ActivityLogService;
import com.groom.moigo.domain.comment.dto.CommentCreateRequest;
import com.groom.moigo.domain.comment.dto.CommentLikeResponse;
import com.groom.moigo.domain.comment.dto.CommentResponse;
import com.groom.moigo.domain.comment.entity.CommentEntity;
import com.groom.moigo.domain.comment.entity.CommentLikeEntity;
import com.groom.moigo.domain.comment.repository.CommentLikeRepository;
import com.groom.moigo.domain.comment.repository.CommentRepository;
import com.groom.moigo.domain.user.entity.UserEntity;
import com.groom.moigo.domain.user.repository.UserRepository;
import com.groom.moigo.global.error.BusinessException;
import com.groom.moigo.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final CommentScheduleLinkReader commentScheduleLinkReader;

    @Override
    public CommentResponse create(
            Long planId,
            Long scheduleId,
            Long userId,
            CommentCreateRequest request
    ) {
        validateScheduleInPlan(planId, scheduleId);
        validateParentComment(request.parentCommentId(), scheduleId);

        UserEntity user = findUser(userId);

        CommentEntity savedComment = commentRepository.save(
                CommentEntity.create(
                        planId,
                        scheduleId,
                        userId,
                        request.content(),
                        request.parentCommentId()
                )
        );

        activityLogService.record(new ActivityRecordCommand(
                planId,
                userId,
                ActivityActionType.COMMENT_CREATED,
                ActivityTargetType.COMMENT,
                savedComment.getCommentId(),
                request.parentCommentId() == null
                        ? "댓글을 남겼어요."
                        : "댓글에 답글을 남겼어요."
        ));

        return CommentResponse.from(savedComment, user, 0, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long planId, Long scheduleId, Long userId) {
        validateScheduleInPlan(planId, scheduleId);

        List<CommentEntity> comments = commentRepository.findByScheduleIdOrderByCreatedAtAsc(scheduleId)
                .stream()
                .filter(comment -> comment.getPlanId().equals(planId))
                .toList();

        Map<Long, UserEntity> usersById = userRepository.findAllById(
                        comments.stream()
                                .filter(comment -> !comment.isDeleted())
                                .map(CommentEntity::getUserId)
                                .distinct()
                                .toList()
                ).stream()
                .collect(Collectors.toMap(UserEntity::getUserId, Function.identity()));

        List<CommentLikeEntity> likes = commentLikeRepository.findByCommentIdIn(
                comments.stream().map(CommentEntity::getCommentId).toList()
        );
        Map<Long, Long> likeCountByCommentId = likes.stream()
                .collect(Collectors.groupingBy(CommentLikeEntity::getCommentId, Collectors.counting()));
        Set<Long> likedCommentIds = likes.stream()
                .filter(like -> like.getUserId().equals(userId))
                .map(CommentLikeEntity::getCommentId)
                .collect(Collectors.toSet());

        return comments.stream()
                .map(comment -> {
                    long likeCount = likeCountByCommentId.getOrDefault(comment.getCommentId(), 0L);
                    boolean likedByMe = likedCommentIds.contains(comment.getCommentId());

                    if (comment.isDeleted()) {
                        return CommentResponse.from(comment, null, likeCount, likedByMe);
                    }

                    UserEntity user = usersById.get(comment.getUserId());
                    if (user == null) {
                        throw new BusinessException(
                                ErrorCode.UNAUTHORIZED,
                                "사용자를 찾을 수 없습니다."
                        );
                    }

                    return CommentResponse.from(comment, user, likeCount, likedByMe);
                })
                .toList();
    }

    @Override
    public void delete(
            Long planId,
            Long scheduleId,
            Long commentId,
            Long userId
    ) {
        validateScheduleInPlan(planId, scheduleId);

        CommentEntity comment = commentRepository.findByCommentIdAndScheduleId(commentId, scheduleId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_INPUT_VALUE,
                            "댓글을 찾을 수 없습니다."
                ));

        if (!comment.getPlanId().equals(planId)) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "해당 계획의 댓글이 아닙니다."
            );
        }

        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "본인이 작성한 댓글만 삭제할 수 있습니다."
            );
        }

        if (comment.isDeleted()) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "이미 삭제된 댓글입니다."
            );
        }

        comment.delete();
        activityLogService.record(new ActivityRecordCommand(
                planId,
                userId,
                ActivityActionType.COMMENT_DELETED,
                ActivityTargetType.COMMENT,
                commentId,
                "댓글을 삭제했어요."
        ));
    }

    @Override
    public CommentLikeResponse toggleLike(
            Long planId,
            Long scheduleId,
            Long commentId,
            Long userId
    ) {
        validateScheduleInPlan(planId, scheduleId);

        // 동시 좋아요 토글 요청의 유니크 제약 위반을 막기 위한 행 잠금
        CommentEntity comment = commentRepository.findByCommentIdAndScheduleIdForUpdate(commentId, scheduleId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "댓글을 찾을 수 없습니다."
                ));

        if (!comment.getPlanId().equals(planId)) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "해당 계획의 댓글이 아닙니다."
            );
        }

        if (comment.isDeleted()) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "삭제된 댓글에는 좋아요를 남길 수 없습니다."
            );
        }

        boolean likedByMe = commentLikeRepository.findByCommentIdAndUserId(commentId, userId)
                .map(existing -> {
                    commentLikeRepository.delete(existing);
                    return false;
                })
                .orElseGet(() -> {
                    commentLikeRepository.save(CommentLikeEntity.create(commentId, userId));
                    return true;
                });

        long likeCount = commentLikeRepository.countByCommentId(commentId);

        // 좋아요를 누른 경우에만 기록함(취소는 기록 안 함)
        if (likedByMe) {
            activityLogService.record(new ActivityRecordCommand(
                    planId,
                    userId,
                    ActivityActionType.COMMENT_LIKED,
                    ActivityTargetType.COMMENT,
                    commentId,
                    "댓글에 좋아요를 눌렀어요."
            ));
        }

        return new CommentLikeResponse(commentId, likeCount, likedByMe);
    }

    private void validateParentComment(Long parentCommentId, Long scheduleId) {
        if (parentCommentId == null) {
            return;
        }

        CommentEntity parentComment = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "부모 댓글을 찾을 수 없습니다."
                ));

        if (!parentComment.getScheduleId().equals(scheduleId)) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "같은 일정의 댓글에만 답글을 작성할 수 있습니다."
            );
        }
    }

    private UserEntity findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.UNAUTHORIZED,
                        "사용자를 찾을 수 없습니다."
                ));
    }

    private void validateScheduleInPlan(Long planId, Long scheduleId) {
        if (!commentScheduleLinkReader.existsInPlan(scheduleId, planId)) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "해당 계획의 일정을 찾을 수 없습니다."
            );
        }
    }
}
