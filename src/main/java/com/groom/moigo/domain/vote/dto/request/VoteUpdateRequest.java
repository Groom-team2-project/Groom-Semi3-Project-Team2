package com.groom.moigo.domain.vote.dto.request;

import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * 투표 수정 요청. null인 필드는 변경하지 않는다.
 *
 * @param title 투표 제목
 * @param description 투표 설명
 * @param deadline 마감 일시(UTC 기준 ISO-8601)
 * @param scheduleId 이 투표가 채우려는 일정 ID
 */
public record VoteUpdateRequest(
		@Size(max = 200, message = "투표 제목은 200자를 넘을 수 없습니다.") String title,
		@Size(max = 1000, message = "투표 설명은 1000자를 넘을 수 없습니다.") String description,
		Instant deadline,
		Long scheduleId) {}
