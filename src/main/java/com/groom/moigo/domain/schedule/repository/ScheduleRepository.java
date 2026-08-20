package com.groom.moigo.domain.schedule.repository;

import com.groom.moigo.domain.schedule.entity.ScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduleRepository extends JpaRepository<ScheduleEntity,Long> {
}
