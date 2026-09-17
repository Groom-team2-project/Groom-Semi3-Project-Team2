package com.groom.moigo.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateRequest(
        @NotBlank(message = "댓글 내용은 비어 있을 수 없습니다.")
        @Size(max = 1000, message = "댓글은 1000자 이하로 입력해주세요.")
        String content,
        Long parentCommentId
) {
}
