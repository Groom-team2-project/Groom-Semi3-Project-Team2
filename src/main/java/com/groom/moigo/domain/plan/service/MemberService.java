package com.groom.moigo.domain.plan.service;

import com.groom.moigo.domain.plan.dto.MemberResponse;
import com.groom.moigo.domain.plan.entity.MemberEntity;
import com.groom.moigo.domain.plan.entity.MemberRole;
import com.groom.moigo.domain.plan.entity.MemberStatus;
import com.groom.moigo.domain.plan.repository.MemberRepository;
import com.groom.moigo.domain.plan.repository.PlanRepository;
import com.groom.moigo.global.error.BusinessException;
import com.groom.moigo.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PlanRepository planRepository;
    private final PlanAccessService planAccessService;

    // 멤버 조회 (참여자만 가능함)
    @Transactional(readOnly = true)
    public List<MemberResponse> getMembers(Long userId, Long planId) {
        requirePlanExists(planId);
        planAccessService.requireJoinedMember(planId, userId);

        return memberRepository.findAllByPlan_PlanIdAndStatus(planId, MemberStatus.JOINED).stream()
                .map(MemberResponse::from)
                .toList();
    }

    // 다른 멤버 권한 변경 (OWNER만 가능함)
    @Transactional
    public MemberResponse changeRole(Long userId, Long planId, Long memberId, MemberRole newRole) {
        requirePlanExists(planId);
        MemberEntity currentMember = planAccessService.requireJoinedMember(planId, userId);
        planAccessService.requireOwner(currentMember); // 일반 멤버가 시도하면 여기서 막힙니다 (테스트 10번 케이스).

        MemberEntity target = getMemberInPlan(planId, memberId);

        if (target.isOwner()) {
            throw new BusinessException(ErrorCode.OWNER_ROLE_CANNOT_BE_CHANGED);
        }
        if (newRole == MemberRole.OWNER) {
            throw new BusinessException(ErrorCode.INVALID_INVITATION_ROLE, "OWNER 권한은 부여할 수 없습니다.");
        }

        target.changeRole(newRole);
        return MemberResponse.from(target);
    }

    // 멤버 내보내기 (OWNER만 가능함)
    @Transactional
    public void removeMember(Long userId, Long planId, Long memberId) {
        requirePlanExists(planId);
        MemberEntity currentMember = planAccessService.requireJoinedMember(planId, userId);
        planAccessService.requireOwner(currentMember);

        MemberEntity target = getMemberInPlan(planId, memberId);
        if (target.isOwner()) {
            throw new BusinessException(ErrorCode.OWNER_CANNOT_BE_REMOVED);
        }
        target.remove();
    }

    // 멤버 스스로 나가기
    @Transactional
    public void leave(Long userId, Long planId) {
        requirePlanExists(planId);
        MemberEntity currentMember = planAccessService.requireJoinedMember(planId, userId);

        if (currentMember.isOwner()) {
            throw new BusinessException(ErrorCode.OWNER_CANNOT_LEAVE);
        }
        currentMember.leave();
    }

    // memberId로 조회
    private MemberEntity getMemberInPlan(Long planId, Long memberId) {
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        //참여중인 멤버인지 조회
        if (!member.getPlan().getPlanId().equals(planId)) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        // LEFT 멤버인지 조회
        if (!member.isJoined()) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }

        return member;
    }

    private void requirePlanExists(Long planId) {
        if (!planRepository.existsById(planId)) {
            throw new BusinessException(ErrorCode.PLAN_NOT_FOUND);
        }
    }
}