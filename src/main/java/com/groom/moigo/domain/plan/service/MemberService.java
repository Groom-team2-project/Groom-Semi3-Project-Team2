package com.groom.moigo.domain.plan.service;

import com.groom.moigo.domain.activity.dto.ActivityRecordCommand;
import com.groom.moigo.domain.activity.entity.ActivityActionType;
import com.groom.moigo.domain.activity.entity.ActivityTargetType;
import com.groom.moigo.domain.activity.service.ActivityLogService;
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
    private final ActivityLogService activityLogService;

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

        recordActivity(
                planId, userId, ActivityActionType.MEMBER_ROLE_CHANGED, target.getMemberId(),
                "멤버 권한을 " + newRole.name() + "(으)로 변경했어요."
        );

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

        // 내보내기와 스스로 나가기는 다른 행위로 해석.
        // 액션 타입은 MEMBER_LEFT로 같지만 요약 문구에서 '누가 누구를'이 드러나도록 구분함
        // 화면은 '{수행자}님이 {요약}' 형태로 그리므로 "A님이 B님을 내보냈어요."로 나옴.
        recordActivity(
                planId, userId, ActivityActionType.MEMBER_LEFT, target.getMemberId(),
                target.getUser().getNickname() + "님을 내보냈어요."
        );
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

        recordActivity(
                planId, userId, ActivityActionType.MEMBER_LEFT, currentMember.getMemberId(),
                "계획에서 나갔어요."
        );
    }

    /**
     * 활동 기록을 남긴다. 기록 저장이 실패해도 멤버 작업 자체는 그대로 성공해야 하므로
     * {@code record()} 안에서 별도 트랜잭션으로 처리되고 예외도 삼켜진다(활동 기록 정책서 5절 2항).
     */
    private void recordActivity(
            Long planId, Long userId, ActivityActionType actionType, Long memberId, String summary) {
        activityLogService.record(new ActivityRecordCommand(
                planId, userId, actionType, ActivityTargetType.MEMBER, memberId, summary));
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
        if (!planRepository.existsByPlanIdAndDeletedAtIsNull(planId)) {
            throw new BusinessException(ErrorCode.PLAN_NOT_FOUND);
        }
    }
}