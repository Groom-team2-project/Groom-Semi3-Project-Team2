package com.groom.moigo.vote.dto.response;

import com.groom.moigo.vote.entity.Vote;
import com.groom.moigo.vote.entity.VoteStatus;
import java.util.List;

/**
 * 투표 집계 결과 응답.
 *
 * @param voteId 투표 ID
 * @param status 투표 상태
 * @param participantCount 참여 인원(회원 기준 중복 제거)
 * @param totalSelectionCount 전체 선택 수(복수 선택 포함)
 * @param resultSummary 결과 요약 문구
 * @param results 선택지별 결과. 득표 수 내림차순
 */
public record VoteResultResponse(
		String voteId,
		VoteStatus status,
		long participantCount,
		long totalSelectionCount,
		String resultSummary,
		List<OptionResult> results) {

	public static VoteResultResponse of(
			Vote vote, long participantCount, String resultSummary, List<OptionResult> results) {
		long totalSelectionCount = results.stream().mapToLong(OptionResult::voteCount).sum();
		return new VoteResultResponse(
				String.valueOf(vote.getId()),
				vote.getStatus(),
				participantCount,
				totalSelectionCount,
				resultSummary,
				results);
	}

	/**
	 * 선택지별 결과.
	 *
	 * @param optionId 선택지 ID
	 * @param placeName 후보 장소 이름
	 * @param placeAddress 후보 장소 주소
	 * @param emoji 후보 장소 이모지
	 * @param placeId 저장된 장소 ID
	 * @param voteCount 득표 수
	 * @param percentage 전체 선택 수 대비 비율(%). 소수점 첫째 자리까지
	 * @param winner 최다 득표 여부. 동점이면 모두 true
	 */
	public record OptionResult(
			String optionId,
			String placeName,
			String placeAddress,
			String emoji,
			String placeId,
			long voteCount,
			double percentage,
			boolean winner) {}
}
