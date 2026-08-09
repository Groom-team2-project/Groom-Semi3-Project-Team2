package com.groom.moigo.domain.plan.dto;

import com.groom.moigo.domain.plan.entity.InvitationEntity;
import com.groom.moigo.domain.plan.entity.InvitationStatus;

import java.time.LocalDateTime;

/**
 * 초대 링크 응답입니다.
 */
public record InvitationResponse(
        Long invitationId,
        String inviteCode,
        InvitationStatus status,
        LocalDateTime expiresAt
) {
    public static InvitationResponse from(InvitationEntity invitation) {
        return new InvitationResponse(
                invitation.getInvitationId(),
                invitation.getInviteCode(),
                invitation.getStatus(),
                invitation.getExpiresAt()
        );
    }
}