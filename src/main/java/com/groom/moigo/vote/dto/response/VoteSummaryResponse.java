package com.groom.moigo.vote.dto.response;

import com.groom.moigo.vote.entity.Vote;
import com.groom.moigo.vote.entity.VoteStatus;
import com.groom.moigo.vote.entity.VoteType;
import java.time.LocalDateTime;

/**
 * 투표 목록용 요약 응답.
 *
 * @param voteId 투표 ID
 * @param title 투표 제목
 * @param type 투표 방식
 * @param status 투표 상태
 * @param closesAt 종료 일시
 * @param createdAt 생성 일시
 * @param optionCount 선택지 수
 * @param participantCount 참여 인원(회원 기준 중복 제거)
 */
public record VoteSummaryResponse(
		Long voteId,
		String title,
		VoteType type,
		VoteStatus status,
		LocalDateTime closesAt,
		LocalDateTime createdAt,
		int optionCount,
		long participantCount) {

	public static VoteSummaryResponse of(Vote vote, int optionCount, long participantCount) {
		return new VoteSummaryResponse(
				vote.getId(),
				vote.getTitle(),
				vote.getType(),
				vote.getStatus(),
				vote.getClosesAt(),
				vote.getCreatedAt(),
				optionCount,
				participantCount);
	}
}
