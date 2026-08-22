package com.groom.moigo.domain.schedule.repository;

import com.groom.moigo.domain.schedule.entity.ScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<ScheduleEntity,Long> {

    //단건 조회
    Optional<ScheduleEntity> findByScheduleIdAndPlanIdAndDeletedAtIsNull(Long scheduleId, Long planId);

    //목록 조회
    List<ScheduleEntity> findAllByPlanIdAndDeletedAtIsNullOrderBySortOrderAsc(Long planId);

    //순서 중복 조회
    boolean existsByPlanIdAndSortOrderAndDeletedAtIsNull(Long planId, Integer sortOrder);
}
