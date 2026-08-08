package com.groom.moigo.vote.dto.request;

import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 투표 수정 요청. null인 필드는 변경하지 않는다.
 *
 * @param title 투표 제목
 * @param description 투표 설명
 * @param closesAt 종료 일시
 */
public record VoteUpdateRequest(
		@Size(max = 100, message = "투표 제목은 100자를 넘을 수 없습니다.") String title,
		@Size(max = 500, message = "투표 설명은 500자를 넘을 수 없습니다.") String description,
		LocalDateTime closesAt) {}
