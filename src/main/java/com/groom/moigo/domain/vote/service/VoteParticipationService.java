package com.groom.moigo.domain.vote.service;

import com.groom.moigo.domain.activity.dto.ActivityRecordCommand;
import com.groom.moigo.domain.activity.entity.ActivityActionType;
import com.groom.moigo.domain.activity.entity.ActivityTargetType;
import com.groom.moigo.domain.activity.service.ActivityLogService;
import com.groom.moigo.domain.vote.dto.request.VoteParticipationRequest;
import com.groom.moigo.domain.vote.dto.response.MyVoteResponse;
import com.groom.moigo.domain.vote.dto.response.VoteResponse;
import com.groom.moigo.domain.vote.dto.response.VoteResultResponse;
import com.groom.moigo.domain.vote.entity.Vote;
import com.groom.moigo.domain.vote.entity.VoteOption;
import com.groom.moigo.domain.vote.entity.VoteParticipation;
import com.groom.moigo.domain.vote.exception.VoteErrorCode;
import com.groom.moigo.domain.vote.exception.VoteException;
import com.groom.moigo.domain.vote.repository.VoteOptionRepository;
import com.groom.moigo.domain.vote.repository.VoteParticipationRepository;
import com.groom.moigo.domain.vote.repository.VoteRepository;
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
 * <p>TODO 계획 멤버만 투표할 수 있도록 하는 검증은 계획·멤버 도메인이 머지되면 추가한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteParticipationService {

	private final VoteRepository voteRepository;
	private final VoteOptionRepository voteOptionRepository;
	private final VoteParticipationRepository voteParticipationRepository;
	private final VoteResponseAssembler assembler;
	private final ActivityLogService activityLogService;

	@Transactional
	public VoteResponse participate(
			Long planId, Long voteId, Long userId, VoteParticipationRequest request) {
		Vote vote = findVoteOfPlan(planId, voteId);
		validateOpen(vote);

		List<Long> optionIds = validateSelection(vote, request.selectedOptionIds());
		List<VoteOption> options = voteOptionRepository.findByVoteIdAndIdIn(voteId, optionIds);
		if (options.size() != optionIds.size()) {
			throw new VoteException(VoteErrorCode.OPTION_NOT_IN_VOTE);
		}

		// 재투표는 기존 선택을 덮어쓴다.
		boolean revote = voteParticipationRepository.deleteByVoteIdAndUserId(voteId, userId) > 0;

		List<VoteParticipation> participations =
				options.stream()
						.map(
								option ->
										VoteParticipation.builder().vote(vote).option(option).userId(userId).build())
						.toList();
		voteParticipationRepository.saveAll(participations);

		// 표를 바꾼 경우는 새로 참여한 것이 아니므로 활동 이력을 남기지 않는다.
		if (!revote) {
			activityLogService.record(
					new ActivityRecordCommand(
							vote.getPlanId(),
							userId,
							ActivityActionType.VOTE_PARTICIPATED,
							ActivityTargetType.VOTE,
							vote.getId(),
							"투표에 참여했어요"));
		}

		return assembler.toVoteResponse(vote, userId);
	}

	@Transactional
	public void cancel(Long planId, Long voteId, Long userId) {
		Vote vote = findVoteOfPlan(planId, voteId);
		validateOpen(vote);

		int deleted = voteParticipationRepository.deleteByVoteIdAndUserId(voteId, userId);
		if (deleted == 0) {
			throw new VoteException(VoteErrorCode.VOTE_PARTICIPATION_NOT_FOUND);
		}
	}

	public MyVoteResponse findMyParticipation(Long planId, Long voteId, Long userId) {
		findVoteOfPlan(planId, voteId);

		List<VoteParticipation> participations =
				voteParticipationRepository.findByVoteIdAndUserIdOrderByIdAsc(voteId, userId);
		if (participations.isEmpty()) {
			throw new VoteException(VoteErrorCode.VOTE_PARTICIPATION_NOT_FOUND);
		}

		List<String> selectedOptionIds =
				participations.stream()
						.map(participation -> String.valueOf(participation.getOption().getId()))
						.toList();
		return MyVoteResponse.of(
				voteId, userId, selectedOptionIds, participations.get(0).getParticipatedAt());
	}

	@Transactional
	public VoteResultResponse findResult(Long planId, Long voteId) {
		Vote vote = findVoteOfPlan(planId, voteId);
		vote.syncStatus(Instant.now());
		return assembler.toResultResponse(vote);
	}

	private void validateOpen(Vote vote) {
		if (vote.isClosed(Instant.now())) {
			throw new VoteException(VoteErrorCode.VOTE_ALREADY_CLOSED);
		}
	}

	private List<Long> validateSelection(Vote vote, List<Long> optionIds) {
		if (optionIds.isEmpty()) {
			throw new VoteException(VoteErrorCode.OPTION_NOT_SELECTED);
		}
		Set<Long> distinct = new HashSet<>(optionIds);
		if (distinct.size() != optionIds.size()) {
			throw new VoteException(VoteErrorCode.DUPLICATED_OPTION_SELECTED);
		}
		if (!vote.getType().allowsMultipleSelection() && optionIds.size() > 1) {
			throw new VoteException(VoteErrorCode.SINGLE_CHOICE_ONLY);
		}
		return optionIds;
	}

	private Vote findVoteOfPlan(Long planId, Long voteId) {
		Vote vote =
				voteRepository
						.findById(voteId)
						.orElseThrow(() -> new VoteException(VoteErrorCode.VOTE_NOT_FOUND));
		if (!vote.belongsToPlan(planId)) {
			throw new VoteException(VoteErrorCode.VOTE_NOT_IN_PLAN);
		}
		return vote;
	}
}
