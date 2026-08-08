package com.groom.moigo.vote.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 투표 선택지 수정 요청.
 *
 * @param placeName 후보 장소 이름
 * @param placeAddress 후보 장소 주소. null이면 기존 값을 유지한다
 * @param emoji 후보 장소 이모지. null이면 기존 값을 유지한다
 * @param placeId 저장된 장소 ID. null을 보내면 장소 연결이 해제된다
 */
public record VoteOptionUpdateRequest(
		@NotBlank(message = "후보 장소 이름은 필수입니다.")
				@Size(max = 200, message = "후보 장소 이름은 200자를 넘을 수 없습니다.")
				String placeName,
		@Size(max = 300, message = "후보 장소 주소는 300자를 넘을 수 없습니다.") String placeAddress,
		@Size(max = 20, message = "이모지는 20자를 넘을 수 없습니다.") String emoji,
		Long placeId) {}
