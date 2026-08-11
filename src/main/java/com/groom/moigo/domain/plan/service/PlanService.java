package com.groom.moigo.domain.plan.service;

import com.groom.moigo.domain.plan.dto.PlanCreateRequest;
import com.groom.moigo.domain.plan.dto.PlanResponse;
import com.groom.moigo.domain.plan.dto.PlanUpdateRequest;
import com.groom.moigo.domain.plan.entity.*;
import com.groom.moigo.domain.plan.repository.InvitationRepository;
import com.groom.moigo.domain.plan.repository.MemberRepository;
import com.groom.moigo.domain.plan.repository.PlanRepository;
import com.groom.moigo.domain.user.entity.UserEntity;
import com.groom.moigo.domain.user.repository.UserRepository;
import com.groom.moigo.global.error.BusinessException;
import com.groom.moigo.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final PlanAccessService planAccessService;
    private final InvitationRepository invitationRepository;

    /**
     * 계획을 생성하고, 생성자를 OWNER 멤버로 자동 등록합니다.
     */
    @Transactional
    public PlanResponse createPlan(Long userId, PlanCreateRequest request) {
        UserEntity owner = getUser(userId);

        PlanEntity plan;
        try {
            plan = PlanEntity.create(
                    owner, request.title(), request.description(),
                    request.startDate(), request.endDate(), request.recruitmentCount()
            );
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            if (message.contains("제목")) {
                throw new BusinessException(ErrorCode.INVALID_PLAN_TITLE, message);
            }
            if (message.contains("모집 인원")) {
                throw new BusinessException(ErrorCode.INVALID_RECRUITMENT_COUNT, message);
            }
            throw new BusinessException(ErrorCode.INVALID_PLAN_DATE, message);
        }

        planRepository.save(plan);
        memberRepository.save(MemberEntity.createOwner(plan, owner));

        return PlanResponse.of(plan, MemberRole.OWNER, 1);
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> getMyPlans(Long userId) {
        return planRepository.findAllJoinedByUserId(userId).stream()
                .map(plan -> {
                    MemberEntity currentMember = memberRepository.findByPlan_PlanIdAndUser_UserId(plan.getPlanId(), userId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));
                    long memberCount = memberRepository.countByPlan_PlanIdAndStatus(plan.getPlanId(), MemberStatus.JOINED);
                    return PlanResponse.of(plan, currentMember.getRole(), memberCount);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanResponse getPlan(Long userId, Long planId) {
        PlanEntity plan = getPlanOrThrow(planId);
        MemberEntity currentMember = planAccessService.requireJoinedMember(planId, userId);
        long memberCount = memberRepository.countByPlan_PlanIdAndStatus(planId, MemberStatus.JOINED);
        return PlanResponse.of(plan, currentMember.getRole(), memberCount);
    }

    @Transactional
    public PlanResponse updatePlan(Long userId, Long planId, PlanUpdateRequest request) {
        PlanEntity plan = getPlanOrThrow(planId);
        MemberEntity currentMember = planAccessService.requireJoinedMember(planId, userId);
        planAccessService.requireEditable(currentMember); // VIEWER는 여기서 막힙니다.

        try {
            plan.update(request.title(), request.description(),
                    request.startDate(), request.endDate(), request.recruitmentCount());
        } catch (IllegalArgumentException e) {
            String message = e.getMessage();
            if (message.contains("제목")) {
                throw new BusinessException(ErrorCode.INVALID_PLAN_TITLE, message);
            }
            if (message.contains("모집 인원")) {
                throw new BusinessException(ErrorCode.INVALID_RECRUITMENT_COUNT, message);
            }
            throw new BusinessException(ErrorCode.INVALID_PLAN_DATE, message);
        }

        long memberCount = memberRepository.countByPlan_PlanIdAndStatus(planId, MemberStatus.JOINED);
        return PlanResponse.of(plan, currentMember.getRole(), memberCount);
    }

    @Transactional
    public void deletePlan(Long userId, Long planId) {
        PlanEntity plan = planRepository.findByIdForUpdate(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));
        MemberEntity currentMember = planAccessService.requireJoinedMember(planId, userId);
        planAccessService.requireOwner(currentMember);

        invitationRepository.findAllByPlan_PlanIdAndStatus(planId, InvitationStatus.ACTIVE)
                .forEach(InvitationEntity::revoke);

        plan.softDelete();
    }

    private PlanEntity getPlanOrThrow(Long planId) {
        return planRepository.findByIdAndNotDeleted(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));
    }

    private UserEntity getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}