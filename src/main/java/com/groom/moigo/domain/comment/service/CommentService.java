package com.groom.moigo.domain.comment.service;

import com.groom.moigo.domain.comment.dto.CommentCreateRequest;
import com.groom.moigo.domain.comment.dto.CommentResponse;

import java.util.List;

public interface CommentService {
    CommentResponse create(
            Long planId,
            Long scheduleId,
            Long userId,
            CommentCreateRequest request
    );

    List<CommentResponse> getComments(Long planId, Long scheduleId);

    void delete (
            Long planId,
            Long scheduleId,
            Long commentId,
            Long userId
    );
}
