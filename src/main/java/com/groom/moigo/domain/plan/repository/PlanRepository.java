package com.groom.moigo.domain.plan.repository;

import com.groom.moigo.domain.plan.entity.PlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<PlanEntity, Long> {

    @Query("""
            select p from PlanEntity p
            join MemberEntity m on m.plan = p
            where m.user.userId = :userId
              and m.status = com.groom.moigo.domain.plan.entity.MemberStatus.JOINED
              and p.deletedAt is null
            order by p.createdAt desc
            """)
    List<PlanEntity> findAllJoinedByUserId(@Param("userId") Long userId);

    @Query("select p from PlanEntity p where p.planId = :planId and p.deletedAt is null")
    Optional<PlanEntity> findByIdAndNotDeleted(@Param("planId") Long planId);

    boolean existsByPlanIdAndDeletedAtIsNull(Long planId);
}