package com.groom.moigo.domain.comment.dto;

import com.groom.moigo.domain.comment.entity.CommentEntity;
import com.groom.moigo.domain.user.entity.UserEntity;

import java.time.LocalDateTime;

public record CommentResponse(
        Long commentId,
        Long planId,
        Long scheduleId,
        Long parentCommentId,
        String content,
        boolean deleted,
        Long userId,
        String nickname,
        String profileImage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long likeCount,
        boolean likedByMe
) {
    public static CommentResponse from(CommentEntity comment, UserEntity user, long likeCount, boolean likedByMe) {
        if (comment.isDeleted()) {
            return new CommentResponse(
                  comment.getCommentId(),
                  comment.getPlanId(),
                  comment.getScheduleId(),
                  comment.getParentCommentId(),
                  "삭제된 댓글입니다.",
                  true,
                  null,
                  null,
                  null,
                  comment.getCreatedAt(),
                  comment.getUpdatedAt(),
                  0,
                  false
            );
        }

        return new CommentResponse(
                comment.getCommentId(),
                comment.getPlanId(),
                comment.getScheduleId(),
                comment.getParentCommentId(),
                comment.getContent(),
                false,
                user.getUserId(),
                user.getNickname(),
                user.getProfileImage(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                likeCount,
                likedByMe
        );
    }
}
