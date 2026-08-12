package com.groom.moigo.domain.comment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class CommentScheduleLinkReader {
    private final JdbcTemplate jdbcTemplate;

    boolean existsInPlan(Long scheduleId, Long planId) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from schedules where schedule_id = ? and plan_id = ?",
                Long.class, scheduleId, planId
        );
        return count != null && count > 0;
    }
}
