package com.groom.moigo.plan.repository;

import com.groom.moigo.plan.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * TODO 계획·멤버·초대 도메인(담당: 주정현) 구현이 머지되면 이 인터페이스를 제거하고 해당 구현을 사용한다.
 *
 * <p>투표 도메인이 계획 존재를 검증하기 위한 임시 스텁이다.
 */
public interface PlanRepository extends JpaRepository<Plan, Long> {}
