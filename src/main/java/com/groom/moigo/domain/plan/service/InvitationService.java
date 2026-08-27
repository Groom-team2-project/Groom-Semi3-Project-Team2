package com.groom.moigo.domain.plan.service;

import com.groom.moigo.domain.plan.dto.InvitationJoinResponse;
import com.groom.moigo.domain.plan.dto.InvitationResponse;
import com.groom.moigo.domain.plan.entity.*;
import com.groom.moigo.domain.plan.repository.InvitationRepository;
import com.groom.moigo.domain.plan.repository.MemberRepository;
import com.groom.moigo.domain.plan.repository.PlanRepository;
import com.groom.moigo.domain.user.entity.UserEntity;
import com.groom.moigo.domain.user.repository.UserRepository;
import com.groom.moigo.global.error.BusinessException;
import com.groom.moigo.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final int MAX_RETRY = 5; // 코드가 우연히 중복될 때 재시도할 최대 횟수
    private static final SecureRandom RANDOM = new SecureRandom();

    private final InvitationRepository invitationRepository;
    private final MemberRepository memberRepository;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final PlanAccessService planAccessService;

   // 초대 링크 발급/재발급
    @Transactional
    public InvitationResponse createInvitation(Long userId, Long planId) {
        PlanEntity plan = getPlanOrThrow(planId);
        MemberEntity currentMember = planAccessService.requireJoinedMember(planId, userId);
        planAccessService.requireOwner(currentMember); // owner만 가능함

        //Active 초대링크인지 확인
        List<InvitationEntity> activeInvitations =
                invitationRepository.findAllByPlan_PlanIdAndStatus(planId, InvitationStatus.ACTIVE);

        if (!activeInvitations.isEmpty()) {
            InvitationEntity existing = activeInvitations.get(0);
            existing.expireIfNeeded(); // 만료 시각 지났는데 상태만 ACTIVE로 남아있으면 여기서 정리
            if (existing.getStatus() == InvitationStatus.ACTIVE) {
                return InvitationResponse.from(existing);
            }
            // 방금 EXPIRED로 바뀌었으면 아래로 내려가서 새로 만듭니다.
        }

        UserEntity inviter = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        InvitationEntity invitation = saveWithUniqueCode(plan, inviter);
        return InvitationResponse.from(invitation);
    }

    // 기존 ACTIVE 링크를 전부 무효화하고 새 링크를 발급
    @Transactional
    public InvitationResponse reissueInvitation(Long userId, Long planId) {
        PlanEntity plan = getPlanOrThrow(planId);
        MemberEntity currentMember = planAccessService.requireJoinedMember(planId, userId);
        planAccessService.requireOwner(currentMember);

        UserEntity inviter = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        invitationRepository.findAllByPlan_PlanIdAndStatus(planId, InvitationStatus.ACTIVE)
                .forEach(InvitationEntity::revoke);

        InvitationEntity invitation = saveWithUniqueCode(plan, inviter);
        return InvitationResponse.from(invitation);
    }

    //owner만 특정 초대 링크 취소 가능
    @Transactional
    public void revokeInvitation(Long userId, Long planId, Long invitationId) {
        getPlanOrThrow(planId);
        MemberEntity currentMember = planAccessService.requireJoinedMember(planId, userId);
        planAccessService.requireOwner(currentMember);

        InvitationEntity invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITATION_NOT_FOUND));

        // 다른 계획의 invitationId를 넣어서 취소를 시도하는 걸 막습니다.
        if (!invitation.getPlan().getPlanId().equals(planId)) {
            throw new BusinessException(ErrorCode.INVITATION_NOT_FOUND);
        }
        invitation.revoke();
    }

    private void expireIfNeeded(InvitationEntity invitation) {
        invitation.expireIfNeeded();
    }

    @Transactional
    public InvitationResponse getInvitationByCode(String inviteCode) {
        InvitationEntity invitation = invitationRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITATION_NOT_FOUND));

        expireIfNeeded(invitation);

        return InvitationResponse.from(invitation);
    }

    // 초대 코드로 계획에 참여합니다.
    // 검증 순서: 코드 존재 여부 -> 취소/만료 여부 -> 중복 참여 여부 -> 정원 초과 여부.
    @Transactional
    public InvitationJoinResponse join(Long userId, String inviteCode) {
        InvitationEntity invitation = invitationRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITATION_NOT_FOUND));

        expireIfNeeded(invitation); //초대링크 만료 여부 확인

        if (invitation.getStatus() == InvitationStatus.REVOKED) {
            throw new BusinessException(ErrorCode.INVITATION_REVOKED);
        }
        if (!invitation.isUsable()) {
            throw new BusinessException(ErrorCode.INVITATION_EXPIRED);
        }

        Long planId = invitation.getPlan().getPlanId();
        PlanEntity plan = planRepository.findByIdForUpdate(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));

        // 여행 종료일이 지난 계획에는 새로 참여할 수 없게 막습니다.
        if (plan.getEndDate().isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.PLAN_ALREADY_COMPLETED);
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        // 참여이력조회
        Optional<MemberEntity> existing = memberRepository.findByPlan_PlanIdAndUser_UserId(planId, userId);

        if (existing.isPresent() && existing.get().isJoined()) {
            throw new BusinessException(ErrorCode.MEMBER_ALREADY_JOINED);
        }

        // 정원 검증
        if (plan.getRecruitmentCount() != null) {
            long currentJoinedCount = memberRepository.countByPlan_PlanIdAndStatus(planId, MemberStatus.JOINED);
            if (currentJoinedCount >= plan.getRecruitmentCount()) {
                throw new BusinessException(ErrorCode.PLAN_RECRUITMENT_FULL);
            }
        }

        MemberEntity member;
        try {
            if (existing.isPresent()) {
                // LEFT 상태
                member = existing.get();
                member.rejoin(MemberRole.EDITOR);
            } else {
                // 신규 참여자
                member = MemberEntity.createFromInvitation(plan, user, MemberRole.EDITOR);
                memberRepository.save(member);
            }
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.MEMBER_ALREADY_JOINED, e);
        }

        return InvitationJoinResponse.from(member);
    }

    private InvitationEntity saveWithUniqueCode(PlanEntity plan, UserEntity inviter) {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            String code = generateCode();
            if (invitationRepository.existsByInviteCode(code)) {
                continue; // 이미 있는 코드면 바로 다음 시도로.
            }
            try {
                // 코드로 참여시 role은 항상 EDITOR
                return invitationRepository.save(
                        InvitationEntity.create(plan, inviter, code, expiresAt)
                );
            } catch (DataIntegrityViolationException e) {
                // 동시에 같은 초대 코드가 저장된 경우 새 코드로 재시도
            }
        }
        throw new BusinessException(ErrorCode.INVITATION_CODE_DUPLICATED);
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private PlanEntity getPlanOrThrow(Long planId) {
        return planRepository.findByIdAndNotDeleted(planId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));
    }

    @Transactional
    public InvitationResponse getCurrentInvitation(Long userId, Long planId) {
        getPlanOrThrow(planId);
        planAccessService.requireJoinedMember(planId, userId); // OWNER 아니어도 JOINED면 통과

        List<InvitationEntity> active = invitationRepository.findAllByPlan_PlanIdAndStatus(planId, InvitationStatus.ACTIVE);
        if (active.isEmpty()) {
            throw new BusinessException(ErrorCode.INVITATION_NOT_FOUND);
        }
        InvitationEntity invitation = active.get(0);
        expireIfNeeded(invitation);
        if (invitation.getStatus() != InvitationStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INVITATION_NOT_FOUND);
        }
        return InvitationResponse.from(invitation);
    }
}