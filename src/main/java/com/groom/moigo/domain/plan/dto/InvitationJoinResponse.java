package com.groom.moigo.domain.plan.dto;

import com.groom.moigo.domain.plan.entity.MemberEntity;
import com.groom.moigo.domain.plan.entity.MemberRole;

/** 초대 코드통해 계획에 참여한 결과를 반환하는 응답입니다. */
public record InvitationJoinResponse(
        Long planId,
        Long memberId,
        MemberRole role
) {
    public static InvitationJoinResponse from(MemberEntity member) {
        return new InvitationJoinResponse(
                member.getPlan().getPlanId(),
                member.getMemberId(),
                member.getRole()
        );
    }
}