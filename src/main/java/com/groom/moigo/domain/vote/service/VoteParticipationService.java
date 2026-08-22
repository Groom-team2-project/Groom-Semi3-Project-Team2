package com.groom.moigo.domain.vote.service;

import com.groom.moigo.domain.plan.service.PlanAccessService;
import com.groom.moigo.domain.vote.dto.request.VoteParticipationRequest;
import com.groom.moigo.domain.vote.dto.response.MyVoteResponse;
import com.groom.moigo.domain.vote.dto.response.VoteResponse;
import com.groom.moigo.domain.vote.dto.response.VoteResultResponse;
import com.groom.moigo.domain.vote.entity.Vote;
import com.groom.moigo.domain.vote.entity.VoteOption;
import com.groom.moigo.domain.vote.entity.VoteParticipation;
import com.groom.moigo.domain.vote.repository.VoteOptionRepository;
import com.groom.moigo.domain.vote.repository.VoteParticipationRepository;
import com.groom.moigo.domain.vote.repository.VoteRepository;
import com.groom.moigo.global.error.BusinessException;
import com.groom.moigo.global.error.ErrorCode;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 투표 참여를 관리한다.
 *
 * <p>이미 참여한 회원이 다시 투표하면 기존 선택을 모두 지우고 새 선택으로 교체한다. 프론트엔드 투표 상세 화면의 "표를 바꿀 수 있어요" 동작이 이 규칙을 따른다.
 *
 * <p>계획에 참여 중인 멤버여야 투표할 수 있다. VIEWER도 참여할 수 있게 두었다. 투표는 계획 내용을 고치는 일이 아니라 의견을 남기는 일이라고 봤다.
 *
 * <p>참여·재투표·취소는 활동 기록을 남기지 않는다. 누가 무엇을 골랐는지 드러나지 않도록 하는 활동 기록 정책을 따른다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteParticipationService {

	private final VoteRepository voteRepository;
	private final VoteOptionRepository voteOptionRepository;
	private final VoteParticipationRepository voteParticipationRepository;
	private final VoteResponseAssembler assembler;
	private final PlanAccessService planAccessService;

	@Transactional
	public VoteResponse participate(
			Long planId, Long voteId, Long userId, VoteParticipationRequest request) {
		planAccessService.requireJoinedMember(planId, userId);
		// 같은 회원의 동시 참여 요청을 줄 세우려고 투표 행에 잠금을 건다.
		Vote vote = findVoteOfPlan(planId, voteId, true);
		validateOpen(vote);

		List<Long> optionIds = validateSelection(vote, request.selectedOptionIds());
		List<VoteOption> options = voteOptionRepository.findByVoteIdAndIdIn(voteId, optionIds);
		if (options.size() != optionIds.size()) {
			throw new BusinessException(ErrorCode.OPTION_NOT_IN_VOTE);
		}

		// 재투표는 기존 선택을 덮어쓴다.
		voteParticipationRepository.deleteByVoteIdAndUserId(voteId, userId);

		List<VoteParticipation> participations =
				options.stream()
						.map(
								option ->
										VoteParticipation.builder().vote(vote).option(option).userId(userId).build())
						.toList();
		voteParticipationRepository.saveAll(participations);

		return assembler.toVoteResponse(vote, userId);
	}

	@Transactional
	public void cancel(Long planId, Long voteId, Long userId) {
		planAccessService.requireJoinedMember(planId, userId);
		Vote vote = findVoteOfPlan(planId, voteId);
		validateOpen(vote);

		int deleted = voteParticipationRepository.deleteByVoteIdAndUserId(voteId, userId);
		if (deleted == 0) {
			throw new BusinessException(ErrorCode.VOTE_PARTICIPATION_NOT_FOUND);
		}
	}

	public MyVoteResponse findMyParticipation(Long planId, Long voteId, Long userId) {
		planAccessService.requireJoinedMember(planId, userId);
		findVoteOfPlan(planId, voteId);

		List<VoteParticipation> participations =
				voteParticipationRepository.findByVoteIdAndUserIdOrderByIdAsc(voteId, userId);
		if (participations.isEmpty()) {
			throw new BusinessException(ErrorCode.VOTE_PARTICIPATION_NOT_FOUND);
		}

		List<String> selectedOptionIds =
				participations.stream()
						.map(participation -> String.valueOf(participation.getOption().getId()))
						.toList();
		return MyVoteResponse.of(
				voteId, userId, selectedOptionIds, participations.get(0).getParticipatedAt());
	}

	@Transactional
	public VoteResultResponse findResult(Long planId, Long voteId, Long userId) {
		planAccessService.requireJoinedMember(planId, userId);
		Vote vote = findVoteOfPlan(planId, voteId);
		vote.syncStatus(Instant.now());
		return assembler.toResultResponse(vote);
	}

	private void validateOpen(Vote vote) {
		if (vote.isClosed(Instant.now())) {
			throw new BusinessException(ErrorCode.VOTE_ALREADY_CLOSED);
		}
	}

	private List<Long> validateSelection(Vote vote, List<Long> optionIds) {
		if (optionIds.isEmpty()) {
			throw new BusinessException(ErrorCode.OPTION_NOT_SELECTED);
		}
		Set<Long> distinct = new HashSet<>(optionIds);
		if (distinct.size() != optionIds.size()) {
			throw new BusinessException(ErrorCode.DUPLICATED_OPTION_SELECTED);
		}
		if (!vote.getType().allowsMultipleSelection() && optionIds.size() > 1) {
			throw new BusinessException(ErrorCode.SINGLE_CHOICE_ONLY);
		}
		return optionIds;
	}

	private Vote findVoteOfPlan(Long planId, Long voteId) {
		return findVoteOfPlan(planId, voteId, false);
	}

	private Vote findVoteOfPlan(Long planId, Long voteId, boolean forUpdate) {
		Vote vote =
				(forUpdate ? voteRepository.findByIdForUpdate(voteId) : voteRepository.findById(voteId))
						.orElseThrow(() -> new BusinessException(ErrorCode.VOTE_NOT_FOUND));
		if (!vote.belongsToPlan(planId)) {
			throw new BusinessException(ErrorCode.VOTE_NOT_IN_PLAN);
		}
		return vote;
	}
}
