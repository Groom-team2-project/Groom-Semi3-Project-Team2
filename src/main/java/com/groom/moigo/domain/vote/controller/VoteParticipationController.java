package com.groom.moigo.domain.vote.controller;

import com.groom.moigo.domain.auth.security.AuthMember;
import com.groom.moigo.domain.vote.dto.request.VoteParticipationRequest;
import com.groom.moigo.domain.vote.dto.response.MyVoteResponse;
import com.groom.moigo.domain.vote.dto.response.VoteResponse;
import com.groom.moigo.domain.vote.dto.response.VoteResultResponse;
import com.groom.moigo.domain.vote.service.VoteParticipationService;
import com.groom.moigo.global.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 투표 참여 API. */
@RestController
@RequestMapping("/api/v1/plans/{planId}/votes/{voteId}")
@RequiredArgsConstructor
public class VoteParticipationController {

	private final VoteParticipationService voteParticipationService;

	/** 투표에 참여한다. 이미 참여했다면 기존 선택을 덮어쓴다. 갱신된 투표 전체를 돌려준다. */
	@PostMapping("/participations")
	public ResponseEntity<CommonResponse<VoteResponse>> participate(
			@PathVariable Long planId,
			@PathVariable Long voteId,
			@AuthenticationPrincipal AuthMember authMember,
			@RequestBody VoteParticipationRequest request) {
		VoteResponse response =
				voteParticipationService.participate(planId, voteId, authMember.userId(), request);
		return ResponseEntity.ok(CommonResponse.success(response, "투표 참여 성공"));
	}

	/** 내 투표 참여를 취소한다. */
	@DeleteMapping("/participations")
	public ResponseEntity<Void> cancel(
			@PathVariable Long planId,
			@PathVariable Long voteId,
			@AuthenticationPrincipal AuthMember authMember) {
		voteParticipationService.cancel(planId, voteId, authMember.userId());
		return ResponseEntity.noContent().build();
	}

	/** 내가 어떤 선택지를 골랐는지 조회한다. */
	@GetMapping("/participations/me")
	public ResponseEntity<CommonResponse<MyVoteResponse>> findMyParticipation(
			@PathVariable Long planId,
			@PathVariable Long voteId,
			@AuthenticationPrincipal AuthMember authMember) {
		MyVoteResponse response =
				voteParticipationService.findMyParticipation(planId, voteId, authMember.userId());
		return ResponseEntity.ok(CommonResponse.success(response, "내 투표 내역 조회 성공"));
	}

	/** 투표 집계 결과를 조회한다. */
	@GetMapping("/result")
	public ResponseEntity<CommonResponse<VoteResultResponse>> findResult(
			@PathVariable Long planId,
			@PathVariable Long voteId,
			@AuthenticationPrincipal AuthMember authMember) {
		VoteResultResponse response =
				voteParticipationService.findResult(planId, voteId, authMember.userId());
		return ResponseEntity.ok(CommonResponse.success(response, "투표 결과 조회 성공"));
	}
}
