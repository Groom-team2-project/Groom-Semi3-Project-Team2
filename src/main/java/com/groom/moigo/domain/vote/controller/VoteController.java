package com.groom.moigo.domain.vote.controller;

import com.groom.moigo.domain.auth.security.AuthMember;
import com.groom.moigo.domain.vote.dto.request.VoteCreateRequest;
import com.groom.moigo.domain.vote.dto.request.VoteOptionCreateRequest;
import com.groom.moigo.domain.vote.dto.request.VoteOptionUpdateRequest;
import com.groom.moigo.domain.vote.dto.request.VoteUpdateRequest;
import com.groom.moigo.domain.vote.dto.response.VoteOptionResponse;
import com.groom.moigo.domain.vote.dto.response.VoteResponse;
import com.groom.moigo.domain.vote.service.VoteService;
import com.groom.moigo.global.response.CommonResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 투표·투표 선택지 API.
 *
 * <p>경로는 프론트엔드 {@code lib/api/votes.ts}가 기대하는 계획 하위 경로를 따르고, 응답은 공통 {@link CommonResponse} 규격으로 감싼다.
 * 프론트엔드가 기대하는 투표 형태는 {@code data} 안에 그대로 들어간다.
 */
@RestController
@RequestMapping("/api/v1/plans/{planId}/votes")
@RequiredArgsConstructor
public class VoteController {

	private final VoteService voteService;

	/** 계획에 투표를 생성한다. */
	@PostMapping
	public ResponseEntity<CommonResponse<VoteResponse>> create(
			@PathVariable Long planId,
			@AuthenticationPrincipal AuthMember authMember,
			@Valid @RequestBody VoteCreateRequest request) {
		VoteResponse response = voteService.create(planId, authMember.userId(), request);
		return ResponseEntity.created(URI.create("/api/v1/plans/" + planId + "/votes/" + response.id()))
				.body(CommonResponse.success(response, "투표 생성 성공"));
	}

	/** 계획에 등록된 투표 목록을 조회한다. 선택지별 득표 수와 내가 고른 선택지가 함께 내려온다. */
	@GetMapping
	public ResponseEntity<CommonResponse<List<VoteResponse>>> findAllByPlan(
			@PathVariable Long planId, @AuthenticationPrincipal AuthMember authMember) {
		List<VoteResponse> response = voteService.findAllByPlan(planId, authMember.userId());
		return ResponseEntity.ok(CommonResponse.success(response, "투표 목록 조회 성공"));
	}

	/** 투표 상세를 조회한다. */
	@GetMapping("/{voteId}")
	public ResponseEntity<CommonResponse<VoteResponse>> findById(
			@PathVariable Long planId,
			@PathVariable Long voteId,
			@AuthenticationPrincipal AuthMember authMember) {
		VoteResponse response = voteService.findById(planId, voteId, authMember.userId());
		return ResponseEntity.ok(CommonResponse.success(response, "투표 조회 성공"));
	}

	/** 투표 제목·설명·마감 일시를 수정한다. 생성자만 가능하다. */
	@PatchMapping("/{voteId}")
	public ResponseEntity<CommonResponse<VoteResponse>> update(
			@PathVariable Long planId,
			@PathVariable Long voteId,
			@AuthenticationPrincipal AuthMember authMember,
			@Valid @RequestBody VoteUpdateRequest request) {
		VoteResponse response = voteService.update(planId, voteId, authMember.userId(), request);
		return ResponseEntity.ok(CommonResponse.success(response, "투표 수정 성공"));
	}

	/** 투표를 삭제한다. 생성자만 가능하며 선택지와 참여 기록도 함께 삭제된다. */
	@DeleteMapping("/{voteId}")
	public ResponseEntity<Void> delete(
			@PathVariable Long planId,
			@PathVariable Long voteId,
			@AuthenticationPrincipal AuthMember authMember) {
		voteService.delete(planId, voteId, authMember.userId());
		return ResponseEntity.noContent().build();
	}

	/** 진행 중인 투표를 즉시 마감한다. 생성자만 가능하다. */
	@PostMapping("/{voteId}/close")
	public ResponseEntity<CommonResponse<VoteResponse>> close(
			@PathVariable Long planId,
			@PathVariable Long voteId,
			@AuthenticationPrincipal AuthMember authMember) {
		VoteResponse response = voteService.close(planId, voteId, authMember.userId());
		return ResponseEntity.ok(CommonResponse.success(response, "투표 마감 성공"));
	}

	/** 선택지를 추가한다. 생성자만 가능하다. */
	@PostMapping("/{voteId}/options")
	public ResponseEntity<CommonResponse<VoteOptionResponse>> addOption(
			@PathVariable Long planId,
			@PathVariable Long voteId,
			@AuthenticationPrincipal AuthMember authMember,
			@Valid @RequestBody VoteOptionCreateRequest request) {
		VoteOptionResponse response =
				voteService.addOption(planId, voteId, authMember.userId(), request);
		return ResponseEntity.created(
						URI.create("/api/v1/plans/" + planId + "/votes/" + voteId + "/options/" + response.id()))
				.body(CommonResponse.success(response, "선택지 추가 성공"));
	}

	/** 선택지를 수정한다. 생성자만 가능하다. */
	@PatchMapping("/{voteId}/options/{optionId}")
	public ResponseEntity<CommonResponse<VoteOptionResponse>> updateOption(
			@PathVariable Long planId,
			@PathVariable Long voteId,
			@PathVariable Long optionId,
			@AuthenticationPrincipal AuthMember authMember,
			@Valid @RequestBody VoteOptionUpdateRequest request) {
		VoteOptionResponse response =
				voteService.updateOption(planId, voteId, optionId, authMember.userId(), request);
		return ResponseEntity.ok(CommonResponse.success(response, "선택지 수정 성공"));
	}

	/** 선택지를 삭제한다. 생성자만 가능하며 선택지가 2개 이하로 줄어들면 삭제할 수 없다. */
	@DeleteMapping("/{voteId}/options/{optionId}")
	public ResponseEntity<Void> deleteOption(
			@PathVariable Long planId,
			@PathVariable Long voteId,
			@PathVariable Long optionId,
			@AuthenticationPrincipal AuthMember authMember) {
		voteService.deleteOption(planId, voteId, optionId, authMember.userId());
		return ResponseEntity.noContent().build();
	}
}
