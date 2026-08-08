package com.groom.moigo.vote.dto.response;

import com.groom.moigo.vote.entity.Vote;
import com.groom.moigo.vote.entity.VoteStatus;
import com.groom.moigo.vote.entity.VoteType;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 투표 상세 응답.
 *
 * @param voteId 투표 ID
 * @param planId 계획 ID
 * @param creatorId 생성자 회원 ID
 * @param title 투표 제목
 * @param description 투표 설명
 * @param type 투표 방식
 * @param status 투표 상태
 * @param closesAt 종료 일시
 * @param createdAt 생성 일시
 * @param participantCount 참여 인원(회원 기준 중복 제거)
 * @param participatedByMe 요청한 회원의 참여 여부
 * @param options 선택지 목록
 */
public record VoteResponse(
		Long voteId,
		Long planId,
		Long creatorId,
		String title,
		String description,
		VoteType type,
		VoteStatus status,
		LocalDateTime closesAt,
		LocalDateTime createdAt,
		long participantCount,
		boolean participatedByMe,
		List<VoteOptionResponse> options) {

	public static VoteResponse of(
			Vote vote,
			long participantCount,
			boolean participatedByMe,
			List<VoteOptionResponse> options) {
		return new VoteResponse(
				vote.getId(),
				vote.getPlan().getId(),
				vote.getCreator().getId(),
				vote.getTitle(),
				vote.getDescription(),
				vote.getType(),
				vote.getStatus(),
				vote.getClosesAt(),
				vote.getCreatedAt(),
				participantCount,
				participatedByMe,
				options);
	}
}
