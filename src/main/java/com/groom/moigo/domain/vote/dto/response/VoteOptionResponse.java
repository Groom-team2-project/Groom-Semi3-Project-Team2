package com.groom.moigo.domain.vote.dto.response;

import com.groom.moigo.domain.vote.entity.VoteOption;

/**
 * 투표 선택지 응답. 프론트엔드 {@code VoteOption} 타입과 필드명을 맞춘다.
 *
 * <p>ID는 프론트엔드 타입이 문자열이므로 문자열로 내려보낸다. DB에는 BIGINT로 저장한다.
 *
 * @param id 선택지 ID
 * @param voteId 투표 ID
 * @param placeName 후보 장소 이름(선택지 내용)
 * @param placeAddress 후보 장소 주소
 * @param emoji 후보 장소 이모지
 * @param voteCount 이 선택지의 득표 수
 * @param placeId 저장된 장소 ID. 장소와 무관한 선택지면 null
 * @param selectedByMe 요청한 회원이 이 선택지를 골랐는지 여부
 */
public record VoteOptionResponse(
		String id,
		String voteId,
		String placeName,
		String placeAddress,
		String emoji,
		long voteCount,
		String placeId,
		boolean selectedByMe) {

	public static VoteOptionResponse of(VoteOption option, long voteCount, boolean selectedByMe) {
		return new VoteOptionResponse(
				String.valueOf(option.getId()),
				String.valueOf(option.getVote().getId()),
				option.getContent(),
				option.getPlaceAddress(),
				option.getEmoji(),
				voteCount,
				option.getPlaceId() == null ? null : String.valueOf(option.getPlaceId()),
				selectedByMe);
	}
}
