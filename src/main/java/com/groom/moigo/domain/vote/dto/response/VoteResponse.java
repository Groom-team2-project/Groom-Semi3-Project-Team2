package com.groom.moigo.domain.vote.dto.response;

import com.groom.moigo.domain.vote.entity.Vote;
import com.groom.moigo.domain.vote.entity.VoteStatus;
import com.groom.moigo.domain.vote.entity.VoteType;
import java.time.Instant;
import java.util.List;

/**
 * 투표 응답. 프론트엔드 {@code Vote} 타입과 필드명을 맞춘다. 목록·상세·투표 참여 모두 같은 형태로 내려간다.
 *
 * <p>{@code creatorId} 아래 필드들은 프론트엔드 화면에서 아직 쓰지 않지만, 투표 수정·복수 선택 같은 백엔드 기능에 필요해 함께 내려보낸다.
 *
 * @param id 투표 ID
 * @param planId 계획 ID
 * @param title 투표 제목
 * @param status 투표 상태(OPEN, CLOSED)
 * @param deadline 마감 일시(UTC 기준 ISO-8601). null이면 생성자가 직접 종료할 때까지 진행
 * @param options 선택지 목록
 * @param myOptionId 요청한 회원이 고른 선택지 ID. 참여 전이면 null
 * @param linkedScheduleId 이 투표가 채우려는 일정 ID. 일정과 무관한 투표면 null
 * @param resultSummary 마감된 투표의 결과 요약. 진행 중이면 null
 * @param creatorId 생성자 회원 ID
 * @param description 투표 설명
 * @param type 투표 방식(SINGLE, MULTIPLE)
 * @param createdAt 생성 일시
 * @param participantCount 참여 인원(회원 기준 중복 제거)
 * @param myOptionIds 요청한 회원이 고른 선택지 ID 전체. 복수 선택 투표용
 */
public record VoteResponse(
		String id,
		String planId,
		String title,
		VoteStatus status,
		Instant deadline,
		List<VoteOptionResponse> options,
		String myOptionId,
		String linkedScheduleId,
		String resultSummary,
		String creatorId,
		String description,
		VoteType type,
		Instant createdAt,
		long participantCount,
		List<String> myOptionIds) {

	public static VoteResponse of(
			Vote vote,
			List<VoteOptionResponse> options,
			List<String> myOptionIds,
			long participantCount,
			String resultSummary) {
		return new VoteResponse(
				String.valueOf(vote.getId()),
				String.valueOf(vote.getPlanId()),
				vote.getTitle(),
				vote.getStatus(),
				vote.getEndDatetime(),
				options,
				myOptionIds.isEmpty() ? null : myOptionIds.get(0),
				vote.getScheduleId() == null ? null : String.valueOf(vote.getScheduleId()),
				resultSummary,
				String.valueOf(vote.getUserId()),
				vote.getDescription(),
				vote.getType(),
				vote.getCreatedAt(),
				participantCount,
				myOptionIds);
	}
}
