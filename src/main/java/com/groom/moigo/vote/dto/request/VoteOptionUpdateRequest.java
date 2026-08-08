package com.groom.moigo.vote.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 투표 선택지 수정 요청.
 *
 * @param content 선택지 내용
 * @param placeId 후보 장소 ID. null을 보내면 장소 연결이 해제된다
 */
public record VoteOptionUpdateRequest(
		@NotBlank(message = "선택지 내용은 필수입니다.")
				@Size(max = 200, message = "선택지 내용은 200자를 넘을 수 없습니다.")
				String content,
		Long placeId) {}
