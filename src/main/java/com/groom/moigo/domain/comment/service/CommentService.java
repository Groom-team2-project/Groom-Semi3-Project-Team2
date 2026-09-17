package com.groom.moigo.domain.comment.service;

import com.groom.moigo.domain.comment.dto.CommentCreateRequest;
import com.groom.moigo.domain.comment.dto.CommentPageResponse;
import com.groom.moigo.domain.comment.dto.CommentLikeResponse;
import com.groom.moigo.domain.comment.dto.CommentResponse;


import java.time.LocalDateTime;

public interface CommentService {
    CommentResponse create(
            Long planId,
            Long scheduleId,
            Long userId,
            CommentCreateRequest request
    );

    CommentPageResponse getComments(
            Long planId,
            Long scheduleId,
            Long userId,
            int size,
            LocalDateTime cursorCreatedAt,
            Long cursorCommentId
    );

    void delete (
            Long planId,
            Long scheduleId,
            Long commentId,
            Long userId
    );

    CommentLikeResponse toggleLike(
            Long planId,
            Long scheduleId,
            Long commentId,
            Long userId
    );
}
