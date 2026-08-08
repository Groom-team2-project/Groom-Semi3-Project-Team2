package com.groom.moigo.vote.service;

import com.groom.moigo.auth.entity.Member;
import com.groom.moigo.auth.repository.MemberRepository;
import com.groom.moigo.vote.dto.request.VoteParticipationRequest;
import com.groom.moigo.vote.dto.response.MyVoteResponse;
import com.groom.moigo.vote.dto.response.VoteResponse;
import com.groom.moigo.vote.dto.response.VoteResultResponse;
import com.groom.moigo.vote.entity.Vote;
import com.groom.moigo.vote.entity.VoteOption;
import com.groom.moigo.vote.entity.VoteParticipation;
import com.groom.moigo.vote.exception.VoteErrorCode;
import com.groom.moigo.vote.exception.VoteException;
import com.groom.moigo.vote.repository.VoteOptionRepository;
import com.groom.moigo.vote.repository.VoteParticipationRepository;
import com.groom.moigo.vote.repository.VoteRepository;
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
	private final MemberRepository memberRepository;
	private final VoteResponseAssembler assembler;

	@Transactional
	public VoteResponse participate(
			Long planId, Long voteId, Long memberId, VoteParticipationRequest request) {
		Vote vote = findVoteOfPlan(planId, voteId);
		validateOpen(vote);

		Member member = findMember(memberId);
		List<Long> optionIds = validateSelection(vote, request.selectedOptionIds());
		List<VoteOption> options = voteOptionRepository.findByVoteIdAndIdIn(voteId, optionIds);
		if (options.size() != optionIds.size()) {
			throw new VoteException(VoteErrorCode.OPTION_NOT_IN_VOTE);
		}

		// 재투표는 기존 선택을 덮어쓴다.
		voteParticipationRepository.deleteByVoteIdAndMemberId(voteId, memberId);

		List<VoteParticipation> participations =
				options.stream()
						.map(
								option ->
										VoteParticipation.builder().vote(vote).option(option).member(member).build())
						.toList();
		voteParticipationRepository.saveAll(participations);

		return assembler.toVoteResponse(vote, memberId);
	}

	@Transactional
	public void cancel(Long planId, Long voteId, Long memberId) {
		Vote vote = findVoteOfPlan(planId, voteId);
		validateOpen(vote);

		int deleted = voteParticipationRepository.deleteByVoteIdAndMemberId(voteId, memberId);
		if (deleted == 0) {
			throw new VoteException(VoteErrorCode.VOTE_PARTICIPATION_NOT_FOUND);
		}
	}

	public MyVoteResponse findMyParticipation(Long planId, Long voteId, Long memberId) {
		findVoteOfPlan(planId, voteId);

		List<VoteParticipation> participations =
				voteParticipationRepository.findByVoteIdAndMemberIdOrderByIdAsc(voteId, memberId);
		if (participations.isEmpty()) {
			throw new VoteException(VoteErrorCode.VOTE_PARTICIPATION_NOT_FOUND);
		}

		List<String> selectedOptionIds =
				participations.stream()
						.map(participation -> String.valueOf(participation.getOption().getId()))
						.toList();
		return MyVoteResponse.of(
				voteId, memberId, selectedOptionIds, participations.get(0).getParticipatedAt());
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

	private Member findMember(Long memberId) {
		return memberRepository
				.findById(memberId)
				.orElseThrow(() -> new VoteException(VoteErrorCode.MEMBER_NOT_FOUND));
	}
}
