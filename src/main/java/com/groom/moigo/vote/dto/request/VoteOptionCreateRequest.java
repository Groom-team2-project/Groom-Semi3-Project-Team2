package com.groom.moigo.vote.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 투표 선택지 생성 요청.
 *
 * <p>프론트엔드는 카카오 장소 검색 결과를 저장하기 전에 투표를 만들기 때문에 {@code placeId} 없이 이름·주소·이모지를 그대로 보낸다. 장소를 먼저 저장한 뒤
 * {@code placeId}를 함께 보내는 흐름도 지원한다.
 *
 * @param placeName 후보 장소 이름. 선택지 내용으로 저장된다
 * @param placeAddress 후보 장소 주소
 * @param emoji 후보 장소 이모지. 비어 있으면 기본 핀 이모지를 쓴다
 * @param placeId 저장된 장소 ID. 장소와 무관한 선택지라면 null
 */
public record VoteOptionCreateRequest(
		@NotBlank(message = "후보 장소 이름은 필수입니다.")
				@Size(max = 200, message = "후보 장소 이름은 200자를 넘을 수 없습니다.")
				String placeName,
		@Size(max = 300, message = "후보 장소 주소는 300자를 넘을 수 없습니다.") String placeAddress,
		@Size(max = 20, message = "이모지는 20자를 넘을 수 없습니다.") String emoji,
		Long placeId) {}
