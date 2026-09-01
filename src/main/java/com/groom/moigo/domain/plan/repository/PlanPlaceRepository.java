package com.groom.moigo.domain.plan.repository;

import com.groom.moigo.domain.plan.entity.PlanPlaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanPlaceRepository extends JpaRepository<PlanPlaceEntity, Long> {

    boolean existsByPlan_PlanIdAndPlaceId(Long planId, Long placeId);

    Optional<PlanPlaceEntity> findByPlan_PlanIdAndPlaceId(Long planId, Long placeId);

    List<PlanPlaceEntity> findAllByPlan_PlanIdOrderByCreatedAtDesc(Long planId);
}
