package com.groom.moigo.domain.plan.repository;

import com.groom.moigo.domain.plan.entity.PlanPlaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlanPlaceRepository extends JpaRepository<PlanPlaceEntity, Long> {

    Optional<PlanPlaceEntity> findByPlan_PlanIdAndPlaceId(Long planId, Long placeId);

    List<PlanPlaceEntity> findAllByPlan_PlanIdOrderByCreatedAtDesc(Long planId);

    @Modifying(flushAutomatically = true)
    @Query(
            value = """
                    INSERT INTO plan_places (plan_id, place_id, created_at)
                    VALUES (:planId, :placeId, CURRENT_TIMESTAMP(6))
                    ON DUPLICATE KEY UPDATE plan_place_id = plan_place_id
                    """,
            nativeQuery = true
    )
    void upsert(@Param("planId") Long planId, @Param("placeId") Long placeId);
}
