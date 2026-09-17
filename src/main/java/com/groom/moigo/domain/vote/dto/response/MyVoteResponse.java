package com.groom.moigo.domain.vote.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * 내 투표 참여 내역 응답.
 *
 * @param voteId 투표 ID
 * @param userId 회원 ID
 * @param selectedOptionId 내가 선택한 선택지 ID. 단일 선택 화면이 쓰는 값
 * @param selectedOptionIds 내가 선택한 선택지 ID 전체
 * @param participatedAt 최초 참여 일시
 */
public record MyVoteResponse(
		String voteId,
		String userId,
		String selectedOptionId,
		List<String> selectedOptionIds,
		Instant participatedAt) {

	public static MyVoteResponse of(
			Long voteId, Long userId, List<String> selectedOptionIds, Instant participatedAt) {
		return new MyVoteResponse(
				String.valueOf(voteId),
				String.valueOf(userId),
				selectedOptionIds.isEmpty() ? null : selectedOptionIds.get(0),
				selectedOptionIds,
				participatedAt);
	}
}
