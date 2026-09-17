package com.groom.moigo.domain.comment.dto;

public record CommentLikeResponse(
        Long commentId,
        long likeCount,
        boolean likedByMe
) {
}
