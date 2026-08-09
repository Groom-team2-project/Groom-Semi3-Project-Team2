package com.groom.moigo.domain.plan.controller;

import com.groom.moigo.domain.auth.security.AuthMember;
import com.groom.moigo.domain.plan.dto.InvitationJoinResponse;
import com.groom.moigo.domain.plan.dto.InvitationResponse;
import com.groom.moigo.domain.plan.service.InvitationService;
import com.groom.moigo.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping("plans/{planId}/invitations")
    public ResponseEntity<CommonResponse<InvitationResponse>> create(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long planId
    ) {
        InvitationResponse response = invitationService.createInvitation(authMember.userId(), planId);
        return ResponseEntity.ok(CommonResponse.success(response, "초대 링크 생성 성공"));
    }

    @PostMapping("plans/{planId}/invitations/reissue")
    public ResponseEntity<CommonResponse<InvitationResponse>> reissue(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long planId
    ) {
        InvitationResponse response = invitationService.reissueInvitation(authMember.userId(), planId);
        return ResponseEntity.ok(CommonResponse.success(response, "초대 링크 재발급 성공"));
    }

    @DeleteMapping("plans/{planId}/invitations/{invitationId}")
    public ResponseEntity<CommonResponse<Void>> revoke(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable Long planId,
            @PathVariable Long invitationId
    ) {
        invitationService.revokeInvitation(authMember.userId(), planId, invitationId);
        return ResponseEntity.ok(CommonResponse.success(null, "초대 링크 취소 성공"));
    }

    @GetMapping("invitations/{inviteCode}")
    public ResponseEntity<CommonResponse<InvitationResponse>> getByCode(
            @PathVariable String inviteCode
    ) {
        InvitationResponse response = invitationService.getInvitationByCode(inviteCode);
        return ResponseEntity.ok(CommonResponse.success(response, "초대 정보 조회 성공"));
    }

    @PostMapping("/invitations/{inviteCode}/join")
    public ResponseEntity<CommonResponse<InvitationJoinResponse>> join(
            @AuthenticationPrincipal AuthMember authMember,
            @PathVariable String inviteCode
    ) {
        InvitationJoinResponse response = invitationService.join(authMember.userId(), inviteCode);
        return ResponseEntity.ok(CommonResponse.success(response, "계획 참여 성공"));
    }
}