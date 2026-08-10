package com.groom.moigo.domain.comment.service;

import com.groom.moigo.domain.activity.dto.ActivityRecordCommand;
import com.groom.moigo.domain.activity.entity.ActivityActionType;
import com.groom.moigo.domain.activity.entity.ActivityTargetType;
import com.groom.moigo.domain.activity.service.ActivityLogService;
import com.groom.moigo.domain.comment.dto.CommentCreateRequest;
import com.groom.moigo.domain.comment.dto.CommentResponse;
import com.groom.moigo.domain.comment.entity.CommentEntity;
import com.groom.moigo.domain.comment.repository.CommentRepository;
import com.groom.moigo.domain.user.entity.UserEntity;
import com.groom.moigo.domain.user.repository.UserRepository;
import com.groom.moigo.global.error.BusinessException;
import com.groom.moigo.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    @Override
    public CommentResponse create(
            Long planId,
            Long scheduleId,
            Long userId,
            CommentCreateRequest request
    ) {
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
                user.getNickname() + "님이 댓글을 남겼어요."
        ));

        return CommentResponse.from(savedComment, user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long planId, Long scheduleId) {
        return commentRepository.findByScheduleIdOrderByCreatedAtAsc(scheduleId)
                .stream()
                .filter(comment -> comment.getPlanId().equals(planId))
                .map(comment -> CommentResponse.from(comment, findUser(comment.getUserId())))
                .toList();
    }

    @Override
    public void delete(
            Long planId,
            Long scheduleId,
            Long commentId,
            Long userId
    ) {
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
}
