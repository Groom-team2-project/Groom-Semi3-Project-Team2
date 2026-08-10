package com.groom.moigo.domain.plan.repository;

import com.groom.moigo.domain.plan.entity.MemberEntity;
import com.groom.moigo.domain.plan.entity.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<MemberEntity, Long> {

    // 이 사람이 계획에 참여중인지 조회
    Optional<MemberEntity> findByPlan_PlanIdAndUser_UserId(Long planId, Long userId);

    // 중복 참여 조회
    boolean existsByPlan_PlanIdAndUser_UserIdAndStatus(Long planId, Long userId, MemberStatus status);

    // 계획에 현재 참여 중인 사람인지 인지 조회
    List<MemberEntity> findAllByPlan_PlanIdAndStatus(Long planId, MemberStatus status);

    // 현재 참여 인원 카운트
    long countByPlan_PlanIdAndStatus(Long planId, MemberStatus status);

    //계획에 참여중/했던 멤버 조회
    List<MemberEntity> findAllByPlan_PlanId(Long planId);
}
