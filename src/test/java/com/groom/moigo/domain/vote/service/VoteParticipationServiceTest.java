package com.groom.moigo.domain.vote.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.groom.moigo.domain.activity.entity.ActivityActionType;
import com.groom.moigo.domain.activity.repository.ActivityLogRepository;
import com.groom.moigo.domain.plan.entity.MemberRole;
import com.groom.moigo.domain.vote.dto.request.VoteCreateRequest;
import com.groom.moigo.domain.vote.dto.request.VoteOptionCreateRequest;
import com.groom.moigo.domain.vote.dto.request.VoteParticipationRequest;
import com.groom.moigo.domain.vote.dto.response.MyVoteResponse;
import com.groom.moigo.domain.vote.dto.response.VoteOptionResponse;
import com.groom.moigo.domain.vote.dto.response.VoteResponse;
import com.groom.moigo.domain.vote.dto.response.VoteResultResponse;
import com.groom.moigo.domain.vote.entity.VoteType;
import com.groom.moigo.domain.vote.exception.VoteErrorCode;
import com.groom.moigo.domain.vote.exception.VoteException;
import com.groom.moigo.domain.vote.support.VoteTestFixture;
import com.groom.moigo.global.error.BusinessException;
import com.groom.moigo.global.error.ErrorCode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

// activityLogService.record()가 REQUIRES_NEW로 커밋되므로, 그 결과를 바로 조회하는 이 테스트는
// READ_COMMITTED로 지정한다 (docs/activity-log-spec.md 7절 참고).
@SpringBootTest
@Transactional(isolation = Isolation.READ_COMMITTED)
class VoteParticipationServiceTest {

	@Autowired private VoteService voteService;
	@Autowired private VoteParticipationService voteParticipationService;
	@Autowired private ActivityLogRepository activityLogRepository;
	@Autowired private VoteTestFixture fixture;

	private Long planId;
	private Long creatorId;
	private Long firstId;
	private Long secondId;

	@BeforeEach
	void setUp() {
		creatorId = fixture.createUser("생성자");
		firstId = fixture.createUser("참여자1");
		secondId = fixture.createUser("참여자2");
		planId = fixture.createPlan(creatorId, "부산 1박 2일");
		fixture.join(planId, firstId, MemberRole.EDITOR);
		fixture.join(planId, secondId, MemberRole.EDITOR);
	}

	@Test
	@DisplayName("VIEWER도 투표에는 참여할 수 있다")
	void participateAsViewer() {
		VoteResponse vote = createVote(VoteType.SINGLE);
		Long viewerId = fixture.createUser("뷰어");
		fixture.join(planId, viewerId, MemberRole.VIEWER);

		VoteResponse result =
				voteParticipationService.participate(
						planId, id(vote.id()), viewerId, single(vote.options().get(0).id()));

		assertThat(result.participantCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("계획 멤버가 아니면 투표에 참여할 수 없다")
	void participateAsNonMember() {
		VoteResponse vote = createVote(VoteType.SINGLE);
		Long outsiderId = fixture.createUser("외부인");
		VoteParticipationRequest request = single(vote.options().get(0).id());

		assertThatThrownBy(
						() -> voteParticipationService.participate(planId, id(vote.id()), outsiderId, request))
				.isInstanceOf(BusinessException.class)
				.extracting(exception -> ((BusinessException) exception).getErrorCode())
				.isEqualTo(ErrorCode.PLAN_ACCESS_DENIED);
	}

	@Test
	@DisplayName("프론트엔드처럼 optionId 하나만 보내도 투표가 반영된다")
	void participateSingleChoice() {
		VoteResponse vote = createVote(VoteType.SINGLE);
		String optionId = vote.options().get(0).id();

		VoteResponse result =
				voteParticipationService.participate(planId, id(vote.id()), firstId, single(optionId));

		assertThat(result.myOptionId()).isEqualTo(optionId);
		assertThat(result.participantCount()).isEqualTo(1);
		assertThat(result.options().get(0).voteCount()).isEqualTo(1);
		assertThat(result.options().get(0).selectedByMe()).isTrue();
		assertThat(result.options().get(1).selectedByMe()).isFalse();
	}

	@Test
	@DisplayName("투표에 참여하면 활동 이력이 남는다")
	void participateRecordsActivity() {
		VoteResponse vote = createVote(VoteType.SINGLE);

		voteParticipationService.participate(
				planId, id(vote.id()), firstId, single(vote.options().get(0).id()));

		// activityLogRepository.save()가 REQUIRES_NEW로 즉시 커밋되어 테스트가 끝나도 롤백되지 않으므로, 이 테이블에는
		// 다른 테스트가 남긴 행이 함께 있을 수 있다. 이번 투표(targetId)로 걸러서 확인한다.
		assertThat(activityLogRepository.findAll())
				.filteredOn(log -> log.getActionType() == ActivityActionType.VOTE_PARTICIPATED
						&& log.getTargetId().equals(Long.valueOf(vote.id())))
				.singleElement()
				.satisfies(
						log -> {
							assertThat(log.getPlanId()).isEqualTo(planId);
							assertThat(log.getUserId()).isEqualTo(firstId);
							assertThat(log.getTargetId()).isEqualTo(Long.valueOf(vote.id()));
							assertThat(log.getSummary()).isEqualTo("투표에 참여했어요");
						});
	}

	@Test
	@DisplayName("표를 바꾸는 재투표는 활동 이력을 새로 남기지 않는다")
	void revoteDoesNotRecordActivityAgain() {
		VoteResponse vote = createVote(VoteType.SINGLE);
		voteParticipationService.participate(
				planId, id(vote.id()), firstId, single(vote.options().get(0).id()));

		voteParticipationService.participate(
				planId, id(vote.id()), firstId, single(vote.options().get(1).id()));

		assertThat(activityLogRepository.findAll())
				.filteredOn(log -> log.getActionType() == ActivityActionType.VOTE_PARTICIPATED
						&& log.getTargetId().equals(Long.valueOf(vote.id())))
				.hasSize(1);
	}

	@Test
	@DisplayName("참여를 취소했다가 다시 참여하면 활동 이력이 새로 남는다")
	void rejoinAfterCancelRecordsActivityAgain() {
		VoteResponse vote = createVote(VoteType.SINGLE);
		voteParticipationService.participate(
				planId, id(vote.id()), firstId, single(vote.options().get(0).id()));
		voteParticipationService.cancel(planId, id(vote.id()), firstId);

		voteParticipationService.participate(
				planId, id(vote.id()), firstId, single(vote.options().get(0).id()));

		// 표를 바꾸는 것과 달리 취소 후 재참여는 새로 참여한 것으로 본다.
		assertThat(activityLogRepository.findAll())
				.filteredOn(log -> log.getActionType() == ActivityActionType.VOTE_PARTICIPATED
						&& log.getTargetId().equals(Long.valueOf(vote.id())))
				.hasSize(2);
	}

	@Test
	@DisplayName("단일 선택 투표에 선택지를 2개 이상 고르면 거부된다")
	void participateSingleChoiceWithMultipleOptions() {
		VoteResponse vote = createVote(VoteType.SINGLE);
		VoteParticipationRequest request = all(vote);

		assertThatThrownBy(
						() -> voteParticipationService.participate(planId, id(vote.id()), firstId, request))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.SINGLE_CHOICE_ONLY);
	}

	@Test
	@DisplayName("복수 선택 투표에는 선택지를 여러 개 고를 수 있다")
	void participateMultipleChoice() {
		VoteResponse vote = createVote(VoteType.MULTIPLE);

		VoteResponse result =
				voteParticipationService.participate(planId, id(vote.id()), firstId, all(vote));

		assertThat(result.participantCount()).isEqualTo(1);
		assertThat(result.options()).allMatch(option -> option.voteCount() == 1);
		assertThat(result.myOptionIds()).hasSize(2);
	}

	@Test
	@DisplayName("선택지를 하나도 보내지 않으면 거부된다")
	void participateWithoutOption() {
		VoteResponse vote = createVote(VoteType.SINGLE);
		VoteParticipationRequest request = new VoteParticipationRequest(null, List.of());

		assertThatThrownBy(
						() -> voteParticipationService.participate(planId, id(vote.id()), firstId, request))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.OPTION_NOT_SELECTED);
	}

	@Test
	@DisplayName("같은 선택지를 중복으로 보내면 거부된다")
	void participateWithDuplicatedOption() {
		VoteResponse vote = createVote(VoteType.MULTIPLE);
		long optionId = id(vote.options().get(0).id());
		VoteParticipationRequest request =
				new VoteParticipationRequest(null, List.of(optionId, optionId));

		assertThatThrownBy(
						() -> voteParticipationService.participate(planId, id(vote.id()), firstId, request))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.DUPLICATED_OPTION_SELECTED);
	}

	@Test
	@DisplayName("다른 투표의 선택지로는 참여할 수 없다")
	void participateWithForeignOption() {
		VoteResponse vote = createVote(VoteType.SINGLE);
		VoteResponse other = createVote(VoteType.SINGLE);
		VoteParticipationRequest request = single(other.options().get(0).id());

		assertThatThrownBy(
						() -> voteParticipationService.participate(planId, id(vote.id()), firstId, request))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.OPTION_NOT_IN_VOTE);
	}

	@Test
	@DisplayName("다른 계획 경로로는 투표에 참여할 수 없다")
	void participateThroughAnotherPlan() {
		VoteResponse vote = createVote(VoteType.SINGLE);
		Long otherPlanId = fixture.createPlan(creatorId, "제주 2박 3일");
		fixture.join(otherPlanId, firstId, MemberRole.EDITOR);
		VoteParticipationRequest request = single(vote.options().get(0).id());

		assertThatThrownBy(
						() ->
								voteParticipationService.participate(otherPlanId, id(vote.id()), firstId, request))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.VOTE_NOT_IN_PLAN);
	}

	@Test
	@DisplayName("다시 투표하면 기존 선택을 덮어쓴다")
	void reParticipateOverwritesPreviousSelection() {
		VoteResponse vote = createVote(VoteType.SINGLE);

		voteParticipationService.participate(
				planId, id(vote.id()), firstId, single(vote.options().get(0).id()));
		VoteResponse result =
				voteParticipationService.participate(
						planId, id(vote.id()), firstId, single(vote.options().get(1).id()));

		assertThat(result.participantCount()).isEqualTo(1);
		assertThat(result.options().get(0).voteCount()).isZero();
		assertThat(result.options().get(1).voteCount()).isEqualTo(1);
		assertThat(result.myOptionId()).isEqualTo(vote.options().get(1).id());
	}

	@Test
	@DisplayName("마감된 투표에는 참여할 수 없다")
	void participateClosedVote() {
		VoteResponse vote = createVote(VoteType.SINGLE);
		voteService.close(planId, id(vote.id()), creatorId);
		VoteParticipationRequest request = single(vote.options().get(0).id());

		assertThatThrownBy(
						() -> voteParticipationService.participate(planId, id(vote.id()), firstId, request))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.VOTE_ALREADY_CLOSED);
	}

	@Test
	@DisplayName("참여를 취소하면 득표 수에서 빠진다")
	void cancelParticipation() {
		VoteResponse vote = createVote(VoteType.SINGLE);
		voteParticipationService.participate(
				planId, id(vote.id()), firstId, single(vote.options().get(0).id()));

		voteParticipationService.cancel(planId, id(vote.id()), firstId);

		VoteResponse found = voteService.findById(planId, id(vote.id()), firstId);
		assertThat(found.participantCount()).isZero();
		assertThat(found.myOptionId()).isNull();
	}

	@Test
	@DisplayName("참여하지 않은 투표는 취소할 수 없다")
	void cancelWithoutParticipation() {
		VoteResponse vote = createVote(VoteType.SINGLE);

		assertThatThrownBy(() -> voteParticipationService.cancel(planId, id(vote.id()), firstId))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.VOTE_PARTICIPATION_NOT_FOUND);
	}

	@Test
	@DisplayName("내 투표 내역에서 내가 고른 선택지를 확인할 수 있다")
	void findMyParticipation() {
		VoteResponse vote = createVote(VoteType.MULTIPLE);
		List<String> optionIds = vote.options().stream().map(VoteOptionResponse::id).toList();
		voteParticipationService.participate(planId, id(vote.id()), firstId, all(vote));

		MyVoteResponse response =
				voteParticipationService.findMyParticipation(planId, id(vote.id()), firstId);

		assertThat(response.memberId()).isEqualTo(String.valueOf(firstId));
		assertThat(response.selectedOptionIds()).containsExactlyInAnyOrderElementsOf(optionIds);
		assertThat(response.selectedOptionId()).isEqualTo(optionIds.get(0));
		assertThat(response.participatedAt()).isNotNull();
	}

	@Test
	@DisplayName("집계 결과는 득표 수 내림차순으로 정렬되고 최다 득표 선택지를 표시한다")
	void findResult() {
		VoteResponse vote = createVote(VoteType.SINGLE);
		String firstOptionId = vote.options().get(0).id();
		String secondOptionId = vote.options().get(1).id();
		voteParticipationService.participate(planId, id(vote.id()), firstId, single(secondOptionId));
		voteParticipationService.participate(planId, id(vote.id()), secondId, single(secondOptionId));
		voteParticipationService.participate(planId, id(vote.id()), creatorId, single(firstOptionId));

		VoteResultResponse result = voteParticipationService.findResult(planId, id(vote.id()), firstId);

		assertThat(result.participantCount()).isEqualTo(3);
		assertThat(result.totalSelectionCount()).isEqualTo(3);
		assertThat(result.resultSummary()).isEqualTo("밀면 2표 · 확정");
		assertThat(result.results().get(0).optionId()).isEqualTo(secondOptionId);
		assertThat(result.results().get(0).placeName()).isEqualTo("밀면");
		assertThat(result.results().get(0).voteCount()).isEqualTo(2);
		assertThat(result.results().get(0).percentage()).isEqualTo(66.7);
		assertThat(result.results().get(0).winner()).isTrue();
		assertThat(result.results().get(1).winner()).isFalse();
	}

	private VoteResponse createVote(VoteType type) {
		return voteService.create(
				planId,
				creatorId,
				new VoteCreateRequest(
						"저녁 뭐 먹을까요",
						null,
						Instant.now().plus(1, ChronoUnit.DAYS),
						type,
						null,
						List.of(
								new VoteOptionCreateRequest("돼지국밥", "부산 서면", "🍲", null),
								new VoteOptionCreateRequest("밀면", "부산 남포동", "🍜", null))));
	}

	/** 프론트엔드 투표 상세 화면이 보내는 단일 선택 요청. */
	private static VoteParticipationRequest single(String optionId) {
		return new VoteParticipationRequest(id(optionId), null);
	}

	private static VoteParticipationRequest all(VoteResponse vote) {
		return new VoteParticipationRequest(
				null, vote.options().stream().map(option -> id(option.id())).toList());
	}

	private static long id(String value) {
		return Long.parseLong(value);
	}
}
