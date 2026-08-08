package com.groom.moigo.vote.dto.request;

import com.groom.moigo.vote.entity.VoteType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 투표 생성 요청.
 *
 * @param title 투표 제목
 * @param description 투표 설명
 * @param type 투표 방식(SINGLE, MULTIPLE)
 * @param closesAt 종료 일시. null이면 생성자가 직접 종료할 때까지 진행
 * @param options 선택지 목록. 최소 2개
 */
public record VoteCreateRequest(
		@NotBlank(message = "투표 제목은 필수입니다.")
				@Size(max = 100, message = "투표 제목은 100자를 넘을 수 없습니다.")
				String title,
		@Size(max = 500, message = "투표 설명은 500자를 넘을 수 없습니다.") String description,
		@NotNull(message = "투표 방식은 필수입니다.") VoteType type,
		LocalDateTime closesAt,
		@NotNull(message = "선택지는 필수입니다.")
				@Size(min = 2, message = "선택지는 최소 2개 이상이어야 합니다.")
				List<@Valid VoteOptionCreateRequest> options) {}
