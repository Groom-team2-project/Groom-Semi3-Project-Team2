package com.groom.moigo.domain.plan.dto;

import com.groom.moigo.domain.plan.entity.MemberEntity;
import com.groom.moigo.domain.plan.entity.MemberRole;
import com.groom.moigo.domain.plan.entity.MemberStatus;

import java.time.LocalDateTime;

public record MemberResponse(
        Long memberId,
        Long userId,
        String nickname,
        String profileImage,
        MemberRole role,
        MemberStatus status,
        LocalDateTime joinedAt
) {
    public static MemberResponse from(MemberEntity member) {
        return new MemberResponse(
                member.getMemberId(),
                member.getUser().getUserId(),
                member.getUser().getNickname(),
                member.getUser().getProfileImage(),
                member.getRole(),
                member.getStatus(),
                member.getJoinedAt()
        );
    }
}