package com.groom.moigo.vote.service;

import com.groom.moigo.auth.entity.Member;
import com.groom.moigo.auth.repository.MemberRepository;
import com.groom.moigo.place.entity.Place;
import com.groom.moigo.place.repository.PlaceRepository;
import com.groom.moigo.plan.entity.Plan;
import com.groom.moigo.plan.repository.PlanRepository;
import com.groom.moigo.vote.dto.request.VoteCreateRequest;
import com.groom.moigo.vote.dto.request.VoteOptionCreateRequest;
import com.groom.moigo.vote.dto.request.VoteOptionUpdateRequest;
import com.groom.moigo.vote.dto.request.VoteUpdateRequest;
import com.groom.moigo.vote.dto.response.VoteOptionResponse;
import com.groom.moigo.vote.dto.response.VoteResponse;
import com.groom.moigo.vote.entity.Vote;
import com.groom.moigo.vote.entity.VoteOption;
import com.groom.moigo.vote.exception.VoteErrorCode;
import com.groom.moigo.vote.exception.VoteException;
import com.groom.moigo.vote.repository.VoteOptionRepository;
import com.groom.moigo.vote.repository.VoteParticipationRepository;
import com.groom.moigo.vote.repository.VoteRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 투표와 투표 선택지를 관리한다.
 *
 * <p>모든 조회·변경은 계획(Plan) 하위에서 이뤄지므로 투표가 해당 계획에 속하는지 먼저 검증한다.
 *
 * <p>TODO 계획 멤버 권한(Plan_Member.권한) 검증은 계획·멤버 도메인이 머지되면 추가한다. 현재는 투표 생성자 본인 여부만 검증한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteService {

	private final VoteRepository voteRepository;
	private final VoteOptionRepository voteOptionRepository;
	private final VoteParticipationRepository voteParticipationRepository;
	private final PlanRepository planRepository;
	private final MemberRepository memberRepository;
	private final PlaceRepository placeRepository;
	private final VoteResponseAssembler assembler;

	@Transactional
	public VoteResponse create(Long planId, Long memberId, VoteCreateRequest request) {
		Plan plan =
				planRepository
						.findById(planId)
						.orElseThrow(() -> new VoteException(VoteErrorCode.PLAN_NOT_FOUND));
		Member creator = findMember(memberId);
		validateRequiredDeadline(request.deadline());

		Vote vote =
				Vote.builder()
						.plan(plan)
						.creator(creator)
						.title(request.title())
						.description(request.description())
						.type(request.typeOrDefault())
						.endDatetime(request.deadline())
						.build();

		for (VoteOptionCreateRequest optionRequest : request.options()) {
			vote.addOption(
					VoteOption.builder()
							.content(optionRequest.placeName())
							.placeAddress(optionRequest.placeAddress())
							.emoji(optionRequest.emoji())
							.place(findPlaceOrNull(optionRequest.placeId()))
							.build());
		}

		Vote saved = voteRepository.save(vote);
		return assembler.toVoteResponse(saved, memberId);
	}

	/** 계획에 등록된 투표를 최신순으로 모두 조회한다. 선택지와 득표 수까지 함께 내려간다. */
	@Transactional
	public List<VoteResponse> findAllByPlan(Long planId, Long memberId) {
		if (!planRepository.existsById(planId)) {
			throw new VoteException(VoteErrorCode.PLAN_NOT_FOUND);
		}

		List<Vote> votes = voteRepository.findByPlanIdOrderByCreatedAtDesc(planId);
		Instant now = Instant.now();
		votes.forEach(vote -> vote.syncStatus(now));

		return assembler.toVoteResponses(votes, memberId);
	}

	@Transactional
	public VoteResponse findById(Long planId, Long voteId, Long memberId) {
		Vote vote = findVoteOfPlan(planId, voteId);
		vote.syncStatus(Instant.now());
		return assembler.toVoteResponse(vote, memberId);
	}

	@Transactional
	public VoteResponse update(Long planId, Long voteId, Long memberId, VoteUpdateRequest request) {
		Vote vote = findVoteOfPlan(planId, voteId);
		validateCreator(vote, memberId);
		validateOpen(vote);
		validateDeadline(request.deadline());

		vote.update(request.title(), request.description(), request.deadline());
		return assembler.toVoteResponse(vote, memberId);
	}

	@Transactional
	public void delete(Long planId, Long voteId, Long memberId) {
		Vote vote = findVoteOfPlan(planId, voteId);
		validateCreator(vote, memberId);

		voteParticipationRepository.deleteByVoteId(voteId);
		voteRepository.deleteById(voteId);
	}

	@Transactional
	public VoteResponse close(Long planId, Long voteId, Long memberId) {
		Vote vote = findVoteOfPlan(planId, voteId);
		validateCreator(vote, memberId);
		validateOpen(vote);

		vote.close();
		return assembler.toVoteResponse(vote, memberId);
	}

	@Transactional
	public VoteOptionResponse addOption(
			Long planId, Long voteId, Long memberId, VoteOptionCreateRequest request) {
		Vote vote = findVoteOfPlan(planId, voteId);
		validateCreator(vote, memberId);
		validateOpen(vote);

		VoteOption option =
				VoteOption.builder()
						.content(request.placeName())
						.placeAddress(request.placeAddress())
						.emoji(request.emoji())
						.place(findPlaceOrNull(request.placeId()))
						.build();
		vote.addOption(option);
		voteOptionRepository.save(option);

		return assembler.toOptionResponse(option, memberId);
	}

	@Transactional
	public VoteOptionResponse updateOption(
			Long planId, Long voteId, Long optionId, Long memberId, VoteOptionUpdateRequest request) {
		Vote vote = findVoteOfPlan(planId, voteId);
		validateCreator(vote, memberId);
		validateOpen(vote);

		VoteOption option = findOptionOf(vote, optionId);
		option.update(
				request.placeName(),
				request.placeAddress(),
				request.emoji(),
				findPlaceOrNull(request.placeId()));

		return assembler.toOptionResponse(option, memberId);
	}

	@Transactional
	public void deleteOption(Long planId, Long voteId, Long optionId, Long memberId) {
		Vote vote = findVoteOfPlan(planId, voteId);
		validateCreator(vote, memberId);
		validateOpen(vote);

		VoteOption option = findOptionOf(vote, optionId);
		if (!vote.canRemoveOption()) {
			throw new VoteException(VoteErrorCode.OPTION_BELOW_MINIMUM);
		}

		// 선택지에 걸린 참여 기록을 먼저 지워야 FK 제약에 걸리지 않는다.
		// 이 호출로 영속성 컨텍스트가 비워지므로 Vote의 cascade가 삭제를 되돌리지 않는다.
		Long targetId = option.getId();
		voteParticipationRepository.deleteByOptionId(targetId);
		voteOptionRepository.deleteById(targetId);
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

	private Place findPlaceOrNull(Long placeId) {
		if (placeId == null) {
			return null;
		}
		return placeRepository
				.findById(placeId)
				.orElseThrow(() -> new VoteException(VoteErrorCode.PLACE_NOT_FOUND));
	}

	private VoteOption findOptionOf(Vote vote, Long optionId) {
		VoteOption option =
				voteOptionRepository
						.findById(optionId)
						.orElseThrow(() -> new VoteException(VoteErrorCode.VOTE_OPTION_NOT_FOUND));
		if (!option.belongsTo(vote.getId())) {
			throw new VoteException(VoteErrorCode.OPTION_NOT_IN_VOTE);
		}
		return option;
	}

	private void validateCreator(Vote vote, Long memberId) {
		if (!vote.isCreatedBy(memberId)) {
			throw new VoteException(VoteErrorCode.NOT_VOTE_CREATOR);
		}
	}

	private void validateOpen(Vote vote) {
		if (vote.isClosed(Instant.now())) {
			throw new VoteException(VoteErrorCode.VOTE_ALREADY_CLOSED);
		}
	}

	/** 수정 요청은 마감 일시를 생략할 수 있다. 보냈다면 미래여야 한다. */
	private void validateDeadline(Instant deadline) {
		if (deadline != null && !deadline.isAfter(Instant.now())) {
			throw new VoteException(VoteErrorCode.INVALID_DEADLINE);
		}
	}

	/** 생성 요청의 마감 일시는 ERD상 필수(VOTES.end_datetime NOT NULL)다. */
	private void validateRequiredDeadline(Instant deadline) {
		if (deadline == null) {
			throw new VoteException(VoteErrorCode.INVALID_DEADLINE);
		}
		validateDeadline(deadline);
	}
}
