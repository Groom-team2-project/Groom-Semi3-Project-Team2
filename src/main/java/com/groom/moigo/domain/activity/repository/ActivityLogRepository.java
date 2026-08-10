package com.groom.moigo.domain.activity.repository;

import com.groom.moigo.domain.activity.entity.ActivityLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogRepository extends  JpaRepository<ActivityLogEntity, Long> {
    List<ActivityLogEntity> findByPlanIdOrderByCreatedAtDesc(Long planId);
}
