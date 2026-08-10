package com.groom.moigo.domain.plan.controller;

import com.groom.moigo.domain.auth.security.AuthMember;
import com.groom.moigo.domain.plan.dto.MemberResponse;
import com.groom.moigo.domain.plan.dto.MemberRoleUpdateRequest;
import com.groom.moigo.domain.plan.service.MemberService;
import com.groom.moigo.global.response.CommonResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plans/{planId}/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping
    public ResponseEntity<CommonResponse<List<MemberResponse>>> getMembers(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long planId
    ) {
        List<MemberResponse> response = memberService.getMembers(authMember.userId(), planId);
        return ResponseEntity.ok(CommonResponse.success(response, "멤버 목록 조회 성공"));
    }

    @PatchMapping("/{memberId}/role")
    public ResponseEntity<CommonResponse<MemberResponse>> changeRole(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long planId,
            @PathVariable Long memberId,
            @Valid @RequestBody MemberRoleUpdateRequest request
    ) {
        MemberResponse response = memberService.changeRole(authMember.userId(), planId, memberId, request.role());
        return ResponseEntity.ok(CommonResponse.success(response, "멤버 권한 변경 성공"));
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<CommonResponse<Void>> removeMember(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long planId,
            @PathVariable Long memberId
    ) {
        memberService.removeMember(authMember.userId(), planId, memberId);
        return ResponseEntity.ok(CommonResponse.success(null, "멤버 내보내기 성공"));
    }

    @DeleteMapping("/me")
    public ResponseEntity<CommonResponse<Void>> leave(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long planId
    ) {
        memberService.leave(authMember.userId(), planId);
        return ResponseEntity.ok(CommonResponse.success(null, "계획 나가기 성공"));
    }
}