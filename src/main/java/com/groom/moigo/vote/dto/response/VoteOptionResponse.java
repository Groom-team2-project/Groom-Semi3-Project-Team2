package com.groom.moigo.vote.dto.response;

import com.groom.moigo.vote.entity.VoteOption;

/**
 * 투표 선택지 응답.
 *
 * @param optionId 선택지 ID
 * @param content 선택지 내용
 * @param placeId 후보 장소 ID. 장소와 무관한 선택지면 null
 * @param voteCount 이 선택지의 득표 수
 * @param selectedByMe 요청한 회원이 이 선택지를 골랐는지 여부
 */
public record VoteOptionResponse(
		Long optionId, String content, Long placeId, long voteCount, boolean selectedByMe) {

	public static VoteOptionResponse of(VoteOption option, long voteCount, boolean selectedByMe) {
		return new VoteOptionResponse(
				option.getId(),
				option.getContent(),
				option.getPlace() == null ? null : option.getPlace().getId(),
				voteCount,
				selectedByMe);
	}
}
