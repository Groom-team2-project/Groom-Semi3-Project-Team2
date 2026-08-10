package com.groom.moigo.domain.vote.dto.request;

import com.groom.moigo.domain.vote.entity.VoteType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/**
 * 투표 생성 요청.
 *
 * <p>프론트엔드 투표 만들기 화면은 {@code title}, {@code deadline}, {@code options}만 보낸다. 나머지는 선택 항목이다.
 *
 * @param title 투표 제목
 * @param description 투표 설명
 * @param deadline 마감 일시(UTC 기준 ISO-8601). ERD의 VOTES.end_datetime이며 필수
 * @param type 투표 방식(SINGLE, MULTIPLE). 생략하면 SINGLE
 * @param scheduleId 이 투표가 채우려는 일정 ID. 일정과 무관한 투표면 생략
 * @param options 선택지 목록. 최소 2개
 */
public record VoteCreateRequest(
		@NotBlank(message = "투표 제목은 필수입니다.")
				@Size(max = 200, message = "투표 제목은 200자를 넘을 수 없습니다.")
				String title,
		@Size(max = 1000, message = "투표 설명은 1000자를 넘을 수 없습니다.") String description,
		@NotNull(message = "마감 일시는 필수입니다.") Instant deadline,
		VoteType type,
		Long scheduleId,
		@NotNull(message = "선택지는 필수입니다.")
				@Size(min = 2, message = "선택지는 최소 2개 이상이어야 합니다.")
				List<@Valid VoteOptionCreateRequest> options) {

	/** 투표 방식을 생략하면 프론트엔드 화면과 같은 1인 1표로 본다. */
	public VoteType typeOrDefault() {
		return type == null ? VoteType.SINGLE : type;
	}
}
