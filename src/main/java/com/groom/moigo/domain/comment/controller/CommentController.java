package com.groom.moigo.domain.comment.controller;

import com.groom.moigo.domain.auth.security.AuthMember;
import com.groom.moigo.domain.comment.dto.CommentCreateRequest;
import com.groom.moigo.domain.comment.dto.CommentLikeResponse;
import com.groom.moigo.domain.comment.dto.CommentResponse;
import com.groom.moigo.domain.comment.service.CommentService;
import com.groom.moigo.domain.plan.entity.MemberEntity;
import com.groom.moigo.domain.plan.service.PlanAccessService;
import com.groom.moigo.global.response.CommonResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plans/{planId}/schedules/{scheduleId}/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;
    private final PlanAccessService planAccessService;

    @PostMapping
    public ResponseEntity<CommonResponse<CommentResponse>> create (
            @PathVariable Long planId,
            @PathVariable Long scheduleId,
            @AuthenticationPrincipal AuthMember authMember,
            @Valid @RequestBody CommentCreateRequest request
    ) {
        Long userId = authMember.userId();
        MemberEntity member = planAccessService.requireJoinedMember(planId, userId);
        planAccessService.requireEditable(member);

        CommentResponse response = commentService.create(planId, scheduleId, userId, request);

        return ResponseEntity.ok(
                CommonResponse.success(response, "댓글 작성 성공")
        );
    }

    @GetMapping
    public ResponseEntity<CommonResponse<List<CommentResponse>>> getComments(
            @PathVariable Long planId,
            @PathVariable Long scheduleId,
            @AuthenticationPrincipal AuthMember authMember
    ) {
        Long userId = authMember.userId();
        planAccessService.requireJoinedMember(planId, userId);

        List<CommentResponse> response = commentService.getComments(planId, scheduleId, userId);

        return ResponseEntity.ok(
                CommonResponse.success(response, "댓글 목록 조회 성공")
        );
    }

    @PostMapping("/{commentId}/likes")
    public ResponseEntity<CommonResponse<CommentLikeResponse>> toggleLike(
            @PathVariable Long planId,
            @PathVariable Long scheduleId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal AuthMember authMember
    ) {
        Long userId = authMember.userId();
        // 좋아요는 열람 권한만 있으면 되는 가벼운 상호작용이라, 편집 권한(requireEditable)까지는 요구하지 않는다.
        planAccessService.requireJoinedMember(planId, userId);

        CommentLikeResponse response = commentService.toggleLike(planId, scheduleId, commentId, userId);

        return ResponseEntity.ok(
                CommonResponse.success(response, "댓글 좋아요 처리 성공")
        );
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<CommonResponse<Void>> delete(
            @PathVariable Long planId,
            @PathVariable Long scheduleId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal AuthMember authMember
    ) {
        Long userId = authMember.userId();
        MemberEntity member = planAccessService.requireJoinedMember(planId, userId);
        planAccessService.requireEditable(member);

        commentService.delete(planId, scheduleId, commentId, userId);

        return ResponseEntity.ok(
                CommonResponse.success(null, "댓글 삭제 성공")
        );
    }
}
