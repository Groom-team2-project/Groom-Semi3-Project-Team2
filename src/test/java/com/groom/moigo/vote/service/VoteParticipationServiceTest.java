package com.groom.moigo.vote.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.groom.moigo.auth.entity.Member;
import com.groom.moigo.auth.repository.MemberRepository;
import com.groom.moigo.plan.entity.Plan;
import com.groom.moigo.plan.repository.PlanRepository;
import com.groom.moigo.vote.dto.request.VoteCreateRequest;
import com.groom.moigo.vote.dto.request.VoteOptionCreateRequest;
import com.groom.moigo.vote.dto.request.VoteParticipationRequest;
import com.groom.moigo.vote.dto.response.MyVoteResponse;
import com.groom.moigo.vote.dto.response.VoteOptionResponse;
import com.groom.moigo.vote.dto.response.VoteResponse;
import com.groom.moigo.vote.dto.response.VoteResultResponse;
import com.groom.moigo.vote.entity.VoteType;
import com.groom.moigo.vote.exception.VoteErrorCode;
import com.groom.moigo.vote.exception.VoteException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class VoteParticipationServiceTest {

	@Autowired private VoteService voteService;
	@Autowired private VoteParticipationService voteParticipationService;
	@Autowired private PlanRepository planRepository;
	@Autowired private MemberRepository memberRepository;

	private Plan plan;
	private Member creator;
	private Member first;
	private Member second;

	@BeforeEach
	void setUp() {
		plan = planRepository.save(new Plan("부산 1박 2일"));
		creator = memberRepository.save(new Member("creator@moigo.com", "생성자", null));
		first = memberRepository.save(new Member("first@moigo.com", "참여자1", null));
		second = memberRepository.save(new Member("second@moigo.com", "참여자2", null));
	}

	@Test
	@DisplayName("프론트엔드처럼 optionId 하나만 보내도 투표가 반영된다")
	void participateSingleChoice() {
		VoteResponse vote = createVote(VoteType.SINGLE);
		String optionId = vote.options().get(0).id();

		VoteResponse result =
				voteParticipationService.participate(
						plan.getId(), id(vote.id()), first.getId(), single(optionId));

		assertThat(result.myOptionId()).isEqualTo(optionId);
		assertThat(result.participantCount()).isEqualTo(1);
		assertThat(result.options().get(0).voteCount()).isEqualTo(1);
		assertThat(result.options().get(0).selectedByMe()).isTrue();
		assertThat(result.options().get(1).selectedByMe()).isFalse();
	}

	@Test
	@DisplayName("단일 선택 투표에 선택지를 2개 이상 고르면 거부된다")
	void participateSingleChoiceWithMultipleOptions() {
		VoteResponse vote = createVote(VoteType.SINGLE);
		VoteParticipationRequest request = all(vote);

		assertThatThrownBy(
						() ->
								voteParticipationService.participate(
										plan.getId(), id(vote.id()), first.getId(), request))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.SINGLE_CHOICE_ONLY);
	}

	@Test
	@DisplayName("복수 선택 투표에는 선택지를 여러 개 고를 수 있다")
	void participateMultipleChoice() {
		VoteResponse vote = createVote(VoteType.MULTIPLE);

		VoteResponse result =
				voteParticipationService.participate(
						plan.getId(), id(vote.id()), first.getId(), all(vote));

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
						() ->
								voteParticipationService.participate(
										plan.getId(), id(vote.id()), first.getId(), request))
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
						() ->
								voteParticipationService.participate(
										plan.getId(), id(vote.id()), first.getId(), request))
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
						() ->
								voteParticipationService.participate(
										plan.getId(), id(vote.id()), first.getId(), request))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.OPTION_NOT_IN_VOTE);
	}

	@Test
	@DisplayName("다른 계획 경로로는 투표에 참여할 수 없다")
	void participateThroughAnotherPlan() {
		VoteResponse vote = createVote(VoteType.SINGLE);
		Plan other = planRepository.save(new Plan("제주 2박 3일"));
		VoteParticipationRequest request = single(vote.options().get(0).id());

		assertThatThrownBy(
						() ->
								voteParticipationService.participate(
										other.getId(), id(vote.id()), first.getId(), request))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.VOTE_NOT_IN_PLAN);
	}

	@Test
	@DisplayName("다시 투표하면 기존 선택을 덮어쓴다")
	void reParticipateOverwritesPreviousSelection() {
		VoteResponse vote = createVote(VoteType.SINGLE);

		voteParticipationService.participate(
				plan.getId(), id(vote.id()), first.getId(), single(vote.options().get(0).id()));
		VoteResponse result =
				voteParticipationService.participate(
						plan.getId(), id(vote.id()), first.getId(), single(vote.options().get(1).id()));

		assertThat(result.participantCount()).isEqualTo(1);
		assertThat(result.options().get(0).voteCount()).isZero();
		assertThat(result.options().get(1).voteCount()).isEqualTo(1);
		assertThat(result.myOptionId()).isEqualTo(vote.options().get(1).id());
	}

	@Test
	@DisplayName("마감된 투표에는 참여할 수 없다")
	void participateClosedVote() {
		VoteResponse vote = createVote(VoteType.SINGLE);
		voteService.close(plan.getId(), id(vote.id()), creator.getId());
		VoteParticipationRequest request = single(vote.options().get(0).id());

		assertThatThrownBy(
						() ->
								voteParticipationService.participate(
										plan.getId(), id(vote.id()), first.getId(), request))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.VOTE_ALREADY_CLOSED);
	}

	@Test
	@DisplayName("참여를 취소하면 득표 수에서 빠진다")
	void cancelParticipation() {
		VoteResponse vote = createVote(VoteType.SINGLE);
		voteParticipationService.participate(
				plan.getId(), id(vote.id()), first.getId(), single(vote.options().get(0).id()));

		voteParticipationService.cancel(plan.getId(), id(vote.id()), first.getId());

		VoteResponse found = voteService.findById(plan.getId(), id(vote.id()), first.getId());
		assertThat(found.participantCount()).isZero();
		assertThat(found.myOptionId()).isNull();
	}

	@Test
	@DisplayName("참여하지 않은 투표는 취소할 수 없다")
	void cancelWithoutParticipation() {
		VoteResponse vote = createVote(VoteType.SINGLE);

		assertThatThrownBy(
						() -> voteParticipationService.cancel(plan.getId(), id(vote.id()), first.getId()))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.VOTE_PARTICIPATION_NOT_FOUND);
	}

	@Test
	@DisplayName("내 투표 내역에서 내가 고른 선택지를 확인할 수 있다")
	void findMyParticipation() {
		VoteResponse vote = createVote(VoteType.MULTIPLE);
		List<String> optionIds = vote.options().stream().map(VoteOptionResponse::id).toList();
		voteParticipationService.participate(plan.getId(), id(vote.id()), first.getId(), all(vote));

		MyVoteResponse response =
				voteParticipationService.findMyParticipation(plan.getId(), id(vote.id()), first.getId());

		assertThat(response.memberId()).isEqualTo(String.valueOf(first.getId()));
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
		voteParticipationService.participate(
				plan.getId(), id(vote.id()), first.getId(), single(secondOptionId));
		voteParticipationService.participate(
				plan.getId(), id(vote.id()), second.getId(), single(secondOptionId));
		voteParticipationService.participate(
				plan.getId(), id(vote.id()), creator.getId(), single(firstOptionId));

		VoteResultResponse result = voteParticipationService.findResult(plan.getId(), id(vote.id()));

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
				plan.getId(),
				creator.getId(),
				new VoteCreateRequest(
						"저녁 뭐 먹을까요",
						null,
						Instant.now().plus(1, ChronoUnit.DAYS),
						type,
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
