package com.groom.moigo.vote.controller;

import com.groom.moigo.vote.dto.request.VoteParticipationRequest;
import com.groom.moigo.vote.dto.response.MyVoteResponse;
import com.groom.moigo.vote.dto.response.VoteResponse;
import com.groom.moigo.vote.dto.response.VoteResultResponse;
import com.groom.moigo.vote.service.VoteParticipationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 투표 참여 API.
 *
 * <p>TODO 회원 식별은 인증 도메인이 머지되면 {@code @AuthenticationPrincipal}로 교체한다.
 */
@RestController
@RequestMapping("/api/v1/plans/{planId}/votes/{voteId}")
@RequiredArgsConstructor
public class VoteParticipationController {

	private static final String MEMBER_ID_HEADER = "X-Member-Id";

	private final VoteParticipationService voteParticipationService;

	/** 투표에 참여한다. 이미 참여했다면 기존 선택을 덮어쓴다. 갱신된 투표 전체를 돌려준다. */
	@PostMapping("/participations")
	public ResponseEntity<VoteResponse> participate(
			@PathVariable Long planId,
			@PathVariable Long voteId,
			@RequestHeader(MEMBER_ID_HEADER) Long memberId,
			@RequestBody VoteParticipationRequest request) {
		return ResponseEntity.ok(
				voteParticipationService.participate(planId, voteId, memberId, request));
	}

	/** 내 투표 참여를 취소한다. */
	@DeleteMapping("/participations")
	public ResponseEntity<Void> cancel(
			@PathVariable Long planId,
			@PathVariable Long voteId,
			@RequestHeader(MEMBER_ID_HEADER) Long memberId) {
		voteParticipationService.cancel(planId, voteId, memberId);
		return ResponseEntity.noContent().build();
	}

	/** 내가 어떤 선택지를 골랐는지 조회한다. */
	@GetMapping("/participations/me")
	public ResponseEntity<MyVoteResponse> findMyParticipation(
			@PathVariable Long planId,
			@PathVariable Long voteId,
			@RequestHeader(MEMBER_ID_HEADER) Long memberId) {
		return ResponseEntity.ok(
				voteParticipationService.findMyParticipation(planId, voteId, memberId));
	}

	/** 투표 집계 결과를 조회한다. */
	@GetMapping("/result")
	public ResponseEntity<VoteResultResponse> findResult(
			@PathVariable Long planId, @PathVariable Long voteId) {
		return ResponseEntity.ok(voteParticipationService.findResult(planId, voteId));
	}
}
