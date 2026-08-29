package com.groom.moigo.domain.plan.repository;

import com.groom.moigo.domain.plan.entity.MemberEntity;
import com.groom.moigo.domain.plan.entity.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<MemberEntity, Long> {

    Optional<MemberEntity> findByPlan_PlanIdAndUser_UserId(Long planId, Long userId);

    List<MemberEntity> findAllByPlan_PlanIdAndStatus(Long planId, MemberStatus status);

    long countByPlan_PlanIdAndStatus(Long planId, MemberStatus status);
}
