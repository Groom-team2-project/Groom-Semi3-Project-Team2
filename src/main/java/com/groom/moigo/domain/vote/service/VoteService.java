package com.groom.moigo.domain.vote.service;

import com.groom.moigo.domain.activity.dto.ActivityRecordCommand;
import com.groom.moigo.domain.activity.entity.ActivityActionType;
import com.groom.moigo.domain.activity.entity.ActivityTargetType;
import com.groom.moigo.domain.activity.service.ActivityLogService;
import com.groom.moigo.domain.plan.entity.MemberEntity;
import com.groom.moigo.domain.plan.service.PlanAccessService;
import com.groom.moigo.domain.vote.dto.request.VoteCreateRequest;
import com.groom.moigo.domain.vote.dto.request.VoteOptionCreateRequest;
import com.groom.moigo.domain.vote.dto.request.VoteOptionUpdateRequest;
import com.groom.moigo.domain.vote.dto.request.VoteUpdateRequest;
import com.groom.moigo.domain.vote.dto.response.VoteOptionResponse;
import com.groom.moigo.domain.vote.dto.response.VoteResponse;
import com.groom.moigo.domain.vote.entity.Vote;
import com.groom.moigo.domain.vote.entity.VoteOption;
import com.groom.moigo.domain.vote.exception.VoteErrorCode;
import com.groom.moigo.domain.vote.exception.VoteException;
import com.groom.moigo.domain.vote.repository.VoteOptionRepository;
import com.groom.moigo.domain.vote.repository.VoteParticipationRepository;
import com.groom.moigo.domain.vote.repository.VoteRepository;
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
 * <p>권한은 세 겹으로 본다. 조회는 계획에 참여 중인 멤버면 되고, 투표를 만드는 일은 EDITOR 이상이어야 하며, 이미 만들어진 투표를 고치는 일은 그 투표를 만든
 * 사람만 할 수 있다.
 *
 * <p>TODO 장소 존재 여부는 장소 도메인이 리포지토리를 제공하기 전까지 마이그레이션의 FK 제약에 맡긴다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteService {

	private final VoteRepository voteRepository;
	private final VoteOptionRepository voteOptionRepository;
	private final VoteParticipationRepository voteParticipationRepository;
	private final VoteResponseAssembler assembler;
	private final ActivityLogService activityLogService;
	private final PlanAccessService planAccessService;
	private final ScheduleLinkReader scheduleLinkReader;

	@Transactional
	public VoteResponse create(Long planId, Long userId, VoteCreateRequest request) {
		requireEditor(planId, userId);
		validateRequiredDeadline(request.deadline());
		validateScheduleInPlan(planId, request.scheduleId());

		Vote vote =
				Vote.builder()
						.planId(planId)
						.userId(userId)
						.scheduleId(request.scheduleId())
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
							.placeId(optionRequest.placeId())
							.build());
		}

		Vote saved = voteRepository.save(vote);
		recordActivity(
				saved,
				userId,
				ActivityActionType.VOTE_CREATED,
				"'%s' 투표를 시작했어요".formatted(saved.getTitle()));

		return assembler.toVoteResponse(saved, userId);
	}

	/** 계획에 등록된 투표를 최신순으로 모두 조회한다. 선택지와 득표 수까지 함께 내려간다. */
	@Transactional
	public List<VoteResponse> findAllByPlan(Long planId, Long userId) {
		planAccessService.requireJoinedMember(planId, userId);

		List<Vote> votes = voteRepository.findByPlanIdOrderByCreatedAtDesc(planId);
		Instant now = Instant.now();
		votes.forEach(vote -> vote.syncStatus(now));

		return assembler.toVoteResponses(votes, userId);
	}

	@Transactional
	public VoteResponse findById(Long planId, Long voteId, Long userId) {
		planAccessService.requireJoinedMember(planId, userId);

		Vote vote = findVoteOfPlan(planId, voteId);
		vote.syncStatus(Instant.now());
		return assembler.toVoteResponse(vote, userId);
	}

	@Transactional
	public VoteResponse update(Long planId, Long voteId, Long userId, VoteUpdateRequest request) {
		Vote vote = findEditableVote(planId, voteId, userId);
		validateOpen(vote);
		validateDeadline(request.deadline());
		validateScheduleInPlan(planId, request.scheduleId());

		vote.update(request.title(), request.description(), request.deadline());
		if (request.scheduleId() != null) {
			vote.linkTo(request.scheduleId());
		}
		recordActivity(
				vote, userId, ActivityActionType.VOTE_UPDATED,
				"'%s' 투표를 수정했어요".formatted(vote.getTitle()));

		return assembler.toVoteResponse(vote, userId);
	}

	@Transactional
	public void delete(Long planId, Long voteId, Long userId) {
		Vote vote = findEditableVote(planId, voteId, userId);
		// 아래 삭제가 영속성 컨텍스트를 비우므로 기록에 쓸 제목을 먼저 담아 둔다.
		String deletedTitle = vote.getTitle();

		voteParticipationRepository.deleteByVoteId(voteId);
		voteRepository.deleteById(voteId);

		recordActivity(
				planId,
				voteId,
				userId,
				ActivityActionType.VOTE_DELETED,
				"'%s' 투표를 삭제했어요".formatted(deletedTitle));
	}

	@Transactional
	public VoteResponse close(Long planId, Long voteId, Long userId) {
		Vote vote = findEditableVote(planId, voteId, userId);
		validateOpen(vote);

		vote.close();
		recordActivity(
				vote,
				userId,
				ActivityActionType.VOTE_CLOSED,
				"'%s' 투표가 마감됐어요".formatted(vote.getTitle()));

		return assembler.toVoteResponse(vote, userId);
	}

	@Transactional
	public VoteOptionResponse addOption(
			Long planId, Long voteId, Long userId, VoteOptionCreateRequest request) {
		Vote vote = findEditableVote(planId, voteId, userId);
		validateOpen(vote);

		VoteOption option =
				VoteOption.builder()
						.content(request.placeName())
						.placeAddress(request.placeAddress())
						.emoji(request.emoji())
						.placeId(request.placeId())
						.build();
		vote.addOption(option);
		voteOptionRepository.save(option);

		recordActivity(
				vote, userId, ActivityActionType.VOTE_UPDATED,
				"'%s' 투표에 후보를 추가했어요".formatted(vote.getTitle()));

		return assembler.toOptionResponse(option, userId);
	}

	@Transactional
	public VoteOptionResponse updateOption(
			Long planId, Long voteId, Long optionId, Long userId, VoteOptionUpdateRequest request) {
		Vote vote = findEditableVote(planId, voteId, userId);
		validateOpen(vote);

		VoteOption option = findOptionOf(vote, optionId);
		option.update(
				request.placeName(),
				request.placeAddress(),
				request.emoji(),
				request.placeId(),
				request.clearPlace());

		recordActivity(
				vote, userId, ActivityActionType.VOTE_UPDATED,
				"'%s' 투표의 후보를 수정했어요".formatted(vote.getTitle()));

		return assembler.toOptionResponse(option, userId);
	}

	@Transactional
	public void deleteOption(Long planId, Long voteId, Long optionId, Long userId) {
		Vote vote = findEditableVote(planId, voteId, userId);
		validateOpen(vote);

		VoteOption option = findOptionOf(vote, optionId);
		if (!vote.canRemoveOption()) {
			throw new VoteException(VoteErrorCode.OPTION_BELOW_MINIMUM);
		}

		// 선택지에 걸린 참여 기록을 먼저 지워야 FK 제약에 걸리지 않는다.
		// 이 호출로 영속성 컨텍스트가 비워지므로 Vote의 cascade가 삭제를 되돌리지 않는다.
		Long targetId = option.getId();
		// 아래 삭제가 영속성 컨텍스트를 비우므로 기록에 쓸 값을 먼저 담아 둔다.
		String removedFrom = vote.getTitle();
		Long ownerPlanId = vote.getPlanId();
		voteParticipationRepository.deleteByOptionId(targetId);
		voteOptionRepository.deleteById(targetId);

		recordActivity(
				ownerPlanId,
				voteId,
				userId,
				ActivityActionType.VOTE_UPDATED,
				"'%s' 투표에서 후보를 뺐어요".formatted(removedFrom));
	}

	/** 이미 만들어진 투표를 고치기 위한 공통 검증. 계획을 편집할 수 있으면서 그 투표를 만든 사람이어야 한다. */
	private Vote findEditableVote(Long planId, Long voteId, Long userId) {
		requireEditor(planId, userId);
		Vote vote = findVoteOfPlan(planId, voteId);
		if (!vote.isCreatedBy(userId)) {
			throw new VoteException(VoteErrorCode.NOT_VOTE_CREATOR);
		}
		return vote;
	}

	/** 투표를 만들거나 고치는 일은 계획을 편집할 수 있는 멤버(OWNER, EDITOR)만 할 수 있다. */
	private void requireEditor(Long planId, Long userId) {
		MemberEntity member = planAccessService.requireJoinedMember(planId, userId);
		planAccessService.requireEditable(member);
	}

	private void recordActivity(
			Long planId, Long voteId, Long userId, ActivityActionType actionType, String summary) {
		activityLogService.record(
				new ActivityRecordCommand(
						planId, userId, actionType, ActivityTargetType.VOTE, voteId, summary));
	}

	private void recordActivity(
			Vote vote, Long userId, ActivityActionType actionType, String summary) {
		activityLogService.record(
				new ActivityRecordCommand(
						vote.getPlanId(), userId, actionType, ActivityTargetType.VOTE, vote.getId(), summary));
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

	/**
	 * 연결하려는 일정이 같은 계획에 속하는지 확인한다.
	 *
	 * <p>FK 제약은 일정 존재 여부만 보므로 다른 계획의 일정에 투표가 붙는 것을 막지 못한다.
	 */
	private void validateScheduleInPlan(Long planId, Long scheduleId) {
		if (scheduleId == null) {
			return;
		}
		if (!scheduleLinkReader.existsInPlan(scheduleId, planId)) {
			throw new VoteException(VoteErrorCode.SCHEDULE_NOT_IN_PLAN);
		}
	}

	/** 생성 요청의 마감 일시는 스키마상 필수(votes.end_datetime NOT NULL)다. */
	private void validateRequiredDeadline(Instant deadline) {
		if (deadline == null) {
			throw new VoteException(VoteErrorCode.INVALID_DEADLINE);
		}
		validateDeadline(deadline);
	}
}
