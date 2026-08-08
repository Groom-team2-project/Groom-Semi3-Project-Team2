package com.groom.moigo.vote.controller;

import com.groom.moigo.vote.dto.request.VoteCreateRequest;
import com.groom.moigo.vote.dto.request.VoteOptionCreateRequest;
import com.groom.moigo.vote.dto.request.VoteOptionUpdateRequest;
import com.groom.moigo.vote.dto.request.VoteUpdateRequest;
import com.groom.moigo.vote.dto.response.VoteOptionResponse;
import com.groom.moigo.vote.dto.response.VoteResponse;
import com.groom.moigo.vote.dto.response.VoteSummaryResponse;
import com.groom.moigo.vote.service.VoteService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 투표·투표 선택지 API.
 *
 * <p>TODO 회원 식별은 인증 도메인이 머지되면 {@code @AuthenticationPrincipal}로 교체한다. 그전까지는 {@code X-Member-Id} 헤더로
 * 요청 회원을 전달받는다.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VoteController {

	private static final String MEMBER_ID_HEADER = "X-Member-Id";

	private final VoteService voteService;

	/** 계획에 투표를 생성한다. */
	@PostMapping("/plans/{planId}/votes")
	public ResponseEntity<VoteResponse> create(
			@PathVariable Long planId,
			@RequestHeader(MEMBER_ID_HEADER) Long memberId,
			@Valid @RequestBody VoteCreateRequest request) {
		VoteResponse response = voteService.create(planId, memberId, request);
		return ResponseEntity.created(URI.create("/api/votes/" + response.voteId())).body(response);
	}

	/** 계획에 등록된 투표 목록을 조회한다. */
	@GetMapping("/plans/{planId}/votes")
	public ResponseEntity<List<VoteSummaryResponse>> findAllByPlan(@PathVariable Long planId) {
		return ResponseEntity.ok(voteService.findAllByPlan(planId));
	}

	/** 투표 상세를 조회한다. 선택지별 득표 수와 내가 고른 선택지가 함께 내려온다. */
	@GetMapping("/votes/{voteId}")
	public ResponseEntity<VoteResponse> findById(
			@PathVariable Long voteId,
			@RequestHeader(value = MEMBER_ID_HEADER, required = false) Long memberId) {
		return ResponseEntity.ok(voteService.findById(voteId, memberId));
	}

	/** 투표 제목·설명·종료 일시를 수정한다. 생성자만 가능하다. */
	@PatchMapping("/votes/{voteId}")
	public ResponseEntity<VoteResponse> update(
			@PathVariable Long voteId,
			@RequestHeader(MEMBER_ID_HEADER) Long memberId,
			@Valid @RequestBody VoteUpdateRequest request) {
		return ResponseEntity.ok(voteService.update(voteId, memberId, request));
	}

	/** 투표를 삭제한다. 생성자만 가능하며 선택지와 참여 기록도 함께 삭제된다. */
	@DeleteMapping("/votes/{voteId}")
	public ResponseEntity<Void> delete(
			@PathVariable Long voteId, @RequestHeader(MEMBER_ID_HEADER) Long memberId) {
		voteService.delete(voteId, memberId);
		return ResponseEntity.noContent().build();
	}

	/** 진행 중인 투표를 즉시 종료한다. 생성자만 가능하다. */
	@PostMapping("/votes/{voteId}/close")
	public ResponseEntity<VoteResponse> close(
			@PathVariable Long voteId, @RequestHeader(MEMBER_ID_HEADER) Long memberId) {
		return ResponseEntity.ok(voteService.close(voteId, memberId));
	}

	/** 선택지를 추가한다. 생성자만 가능하다. */
	@PostMapping("/votes/{voteId}/options")
	public ResponseEntity<VoteOptionResponse> addOption(
			@PathVariable Long voteId,
			@RequestHeader(MEMBER_ID_HEADER) Long memberId,
			@Valid @RequestBody VoteOptionCreateRequest request) {
		VoteOptionResponse response = voteService.addOption(voteId, memberId, request);
		return ResponseEntity.created(
						URI.create("/api/votes/" + voteId + "/options/" + response.optionId()))
				.body(response);
	}

	/** 선택지를 수정한다. 생성자만 가능하다. */
	@PatchMapping("/votes/{voteId}/options/{optionId}")
	public ResponseEntity<VoteOptionResponse> updateOption(
			@PathVariable Long voteId,
			@PathVariable Long optionId,
			@RequestHeader(MEMBER_ID_HEADER) Long memberId,
			@Valid @RequestBody VoteOptionUpdateRequest request) {
		return ResponseEntity.ok(voteService.updateOption(voteId, optionId, memberId, request));
	}

	/** 선택지를 삭제한다. 생성자만 가능하며 선택지가 2개 이하로 줄어들면 삭제할 수 없다. */
	@DeleteMapping("/votes/{voteId}/options/{optionId}")
	public ResponseEntity<Void> deleteOption(
			@PathVariable Long voteId,
			@PathVariable Long optionId,
			@RequestHeader(MEMBER_ID_HEADER) Long memberId) {
		voteService.deleteOption(voteId, optionId, memberId);
		return ResponseEntity.noContent().build();
	}
}
