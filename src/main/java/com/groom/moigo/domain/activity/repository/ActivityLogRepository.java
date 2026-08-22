package com.groom.moigo.domain.activity.repository;

import com.groom.moigo.domain.activity.entity.ActivityActionType;
import com.groom.moigo.domain.activity.entity.ActivityLogEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLogEntity, Long> {
    @Query("""
            select activity from ActivityLogEntity activity
            where activity.planId = :planId
              and activity.actionType in :actionTypes
              and (
                    :cursorCreatedAt is null
                    or activity.createdAt < :cursorCreatedAt
                    or (activity.createdAt = :cursorCreatedAt and activity.logId < :cursorLogId)
              )
            order by activity.createdAt desc, activity.logId desc
            """)
    List<ActivityLogEntity> findSharedActivitiesByCursor(
            @Param("planId") Long planId,
            @Param("actionTypes") List<ActivityActionType> actionTypes,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorLogId") Long cursorLogId,
            Pageable pageable
    );

    @Query("""
            select activity from ActivityLogEntity activity
            where activity.userId = :userId
              and (
                    :cursorCreatedAt is null
                    or activity.createdAt < :cursorCreatedAt
                    or (activity.createdAt = :cursorCreatedAt and activity.logId < :cursorLogId)
              )
            order by activity.createdAt desc, activity.logId desc
            """)
    List<ActivityLogEntity> findMyActivitiesByCursor(
            @Param("userId") Long userId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorLogId") Long cursorLogId,
            Pageable pageable
    );
}
