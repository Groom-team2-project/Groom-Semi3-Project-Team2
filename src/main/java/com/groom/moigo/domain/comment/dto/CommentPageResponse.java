package com.groom.moigo.domain.comment.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 댓글 목록 한 페이지.
 *
 * <p>커서는 이 페이지 마지막 <b>부모 댓글</b>의 위치를 가리킨다. 대댓글은 부모에 딸려 함께 내려가므로
 * 커서 대상이 아니다. 트리가 페이지 경계에서 잘리지 않도록 하기 위한 선택이다.
 */
public record CommentPageResponse(
        List<CommentResponse> comments,
        LocalDateTime nextCursorCreatedAt,
        Long nextCursorCommentId,
        boolean hasNext
) {
}
