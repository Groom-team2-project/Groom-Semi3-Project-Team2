package com.groom.moigo.domain.plan.repository;

import com.groom.moigo.domain.plan.entity.InvitationEntity;
import com.groom.moigo.domain.plan.entity.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<InvitationEntity, Long> {

    // 초대 코드 조회
    Optional<InvitationEntity> findByInviteCode(String inviteCode);

    List<InvitationEntity> findAllByPlan_PlanIdAndStatus(Long planId, InvitationStatus status);

    boolean existsByInviteCode(String inviteCode);
}