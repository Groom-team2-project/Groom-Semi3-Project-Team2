package com.groom.moigo.domain.plan.service;

import com.groom.moigo.domain.plan.entity.MemberEntity;
import com.groom.moigo.domain.plan.entity.MemberRole;
import com.groom.moigo.domain.plan.entity.MemberStatus;
import com.groom.moigo.domain.plan.repository.MemberRepository;
import com.groom.moigo.global.error.BusinessException;
import com.groom.moigo.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Plan 접근/권한 관련
 */
@Service
@RequiredArgsConstructor
public class PlanAccessService {

    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public boolean isJoinedMember(Long planId, Long userId) {
        return memberRepository.existsByPlan_PlanIdAndUser_UserIdAndStatus(planId, userId, MemberStatus.JOINED);
    }

    @Transactional(readOnly = true)
    public MemberRole getRole(Long planId, Long userId) {
        return memberRepository.findByPlan_PlanIdAndUser_UserId(planId, userId)
                .filter(MemberEntity::isJoined)
                .map(MemberEntity::getRole)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean canEdit(Long planId, Long userId) {
        MemberRole role = getRole(planId, userId);
        return role == MemberRole.OWNER || role == MemberRole.EDITOR;
    }

    // 유저 상태 체크 (JOINED)
    public MemberEntity requireJoinedMember(Long planId, Long userId) {
        MemberEntity member = memberRepository.findByPlan_PlanIdAndUser_UserId(planId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_ACCESS_DENIED));

        if (!member.isJoined()) {
            throw new BusinessException(ErrorCode.PLAN_ACCESS_DENIED);
        }
        return member;
    }

    // 조회 가능 여부
    public void requireEditable(MemberEntity member) {
        if (member.getRole() == MemberRole.VIEWER) {
            throw new BusinessException(ErrorCode.PLAN_UPDATE_FORBIDDEN);
        }
    }

    // OWNER 권한 확인
    public void requireOwner(MemberEntity member) {
        if (!member.isOwner()) {
            throw new BusinessException(ErrorCode.MEMBER_ACCESS_DENIED);
        }
    }
}