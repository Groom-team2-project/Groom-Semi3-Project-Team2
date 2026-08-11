package com.groom.moigo.domain.vote.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 투표에 연결하려는 일정이 같은 계획에 속하는지 확인한다.
 *
 * <p>FK 제약은 일정이 존재하는지만 보장하므로 다른 계획의 일정 ID를 보내도 통과한다. 그래서 계획 소속까지 직접 확인한다.
 *
 * <p>TODO 일정 도메인(담당: 박소빈)이 리포지토리를 제공하면 이 클래스를 지우고 그쪽 조회를 쓴다. 그때까지만 테이블을 직접 읽는다.
 */
@Component
@RequiredArgsConstructor
class ScheduleLinkReader {

	private final JdbcTemplate jdbcTemplate;

	boolean existsInPlan(Long scheduleId, Long planId) {
		Long count =
				jdbcTemplate.queryForObject(
						"select count(*) from schedules where schedule_id = ? and plan_id = ?",
						Long.class,
						scheduleId,
						planId);
		return count != null && count > 0;
	}
}
