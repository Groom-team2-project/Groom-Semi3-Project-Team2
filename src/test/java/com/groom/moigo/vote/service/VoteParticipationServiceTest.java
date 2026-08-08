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
import com.groom.moigo.vote.dto.response.VoteResponse;
import com.groom.moigo.vote.dto.response.VoteResultResponse;
import com.groom.moigo.vote.entity.VoteType;
import com.groom.moigo.vote.exception.VoteErrorCode;
import com.groom.moigo.vote.exception.VoteException;
import java.time.LocalDateTime;
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
	@DisplayName("단일 선택 투표에 참여하면 득표 수와 내 선택이 반영된다")
	void participateSingleChoice() {
		VoteResponse vote = createVote(VoteType.SINGLE);
		Long optionId = vote.options().get(0).optionId();

		VoteResponse result =
				voteParticipationService.participate(
						vote.voteId(), first.getId(), new VoteParticipationRequest(List.of(optionId)));

		assertThat(result.participatedByMe()).isTrue();
		assertThat(result.participantCount()).isEqualTo(1);
		assertThat(result.options().get(0).voteCount()).isEqualTo(1);
		assertThat(result.options().get(0).selectedByMe()).isTrue();
		assertThat(result.options().get(1).selectedByMe()).isFalse();
	}

	@Test
	@DisplayName("단일 선택 투표에 선택지를 2개 이상 고르면 거부된다")
	void participateSingleChoiceWithMultipleOptions() {
		VoteResponse vote = createVote(VoteType.SINGLE);
		List<Long> optionIds =
				vote.options().stream().map(option -> option.optionId()).toList();

		assertThatThrownBy(
						() ->
								voteParticipationService.participate(
										vote.voteId(), first.getId(), new VoteParticipationRequest(optionIds)))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.SINGLE_CHOICE_ONLY);
	}

	@Test
	@DisplayName("복수 선택 투표에는 선택지를 여러 개 고를 수 있다")
	void participateMultipleChoice() {
		VoteResponse vote = createVote(VoteType.MULTIPLE);
		List<Long> optionIds =
				vote.options().stream().map(option -> option.optionId()).toList();

		VoteResponse result =
				voteParticipationService.participate(
						vote.voteId(), first.getId(), new VoteParticipationRequest(optionIds));

		assertThat(result.participantCount()).isEqualTo(1);
		assertThat(result.options()).allMatch(option -> option.voteCount() == 1);
	}

	@Test
	@DisplayName("같은 선택지를 중복으로 보내면 거부된다")
	void participateWithDuplicatedOption() {
		VoteResponse vote = createVote(VoteType.MULTIPLE);
		Long optionId = vote.options().get(0).optionId();

		assertThatThrownBy(
						() ->
								voteParticipationService.participate(
										vote.voteId(),
										first.getId(),
										new VoteParticipationRequest(List.of(optionId, optionId))))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.DUPLICATED_OPTION_SELECTED);
	}

	@Test
	@DisplayName("다른 투표의 선택지로는 참여할 수 없다")
	void participateWithForeignOption() {
		VoteResponse vote = createVote(VoteType.SINGLE);
		VoteResponse other = createVote(VoteType.SINGLE);
		Long foreignOptionId = other.options().get(0).optionId();

		assertThatThrownBy(
						() ->
								voteParticipationService.participate(
										vote.voteId(),
										first.getId(),
										new VoteParticipationRequest(List.of(foreignOptionId))))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.OPTION_NOT_IN_VOTE);
	}

	@Test
	@DisplayName("다시 투표하면 기존 선택을 덮어쓴다")
	void reParticipateOverwritesPreviousSelection() {
		VoteResponse vote = createVote(VoteType.SINGLE);
		Long firstOptionId = vote.options().get(0).optionId();
		Long secondOptionId = vote.options().get(1).optionId();

		voteParticipationService.participate(
				vote.voteId(), first.getId(), new VoteParticipationRequest(List.of(firstOptionId)));
		VoteResponse result =
				voteParticipationService.participate(
						vote.voteId(), first.getId(), new VoteParticipationRequest(List.of(secondOptionId)));

		assertThat(result.participantCount()).isEqualTo(1);
		assertThat(result.options().get(0).voteCount()).isZero();
		assertThat(result.options().get(1).voteCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("종료된 투표에는 참여할 수 없다")
	void participateClosedVote() {
		VoteResponse vote = createVote(VoteType.SINGLE);
		voteService.close(vote.voteId(), creator.getId());
		Long optionId = vote.options().get(0).optionId();

		assertThatThrownBy(
						() ->
								voteParticipationService.participate(
										vote.voteId(), first.getId(), new VoteParticipationRequest(List.of(optionId))))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.VOTE_ALREADY_CLOSED);
	}

	@Test
	@DisplayName("참여를 취소하면 득표 수에서 빠진다")
	void cancelParticipation() {
		VoteResponse vote = createVote(VoteType.SINGLE);
		Long optionId = vote.options().get(0).optionId();
		voteParticipationService.participate(
				vote.voteId(), first.getId(), new VoteParticipationRequest(List.of(optionId)));

		voteParticipationService.cancel(vote.voteId(), first.getId());

		VoteResponse found = voteService.findById(vote.voteId(), first.getId());
		assertThat(found.participantCount()).isZero();
		assertThat(found.participatedByMe()).isFalse();
	}

	@Test
	@DisplayName("참여하지 않은 투표는 취소할 수 없다")
	void cancelWithoutParticipation() {
		VoteResponse vote = createVote(VoteType.SINGLE);

		assertThatThrownBy(() -> voteParticipationService.cancel(vote.voteId(), first.getId()))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.VOTE_PARTICIPATION_NOT_FOUND);
	}

	@Test
	@DisplayName("내 투표 내역에서 내가 고른 선택지를 확인할 수 있다")
	void findMyParticipation() {
		VoteResponse vote = createVote(VoteType.MULTIPLE);
		List<Long> optionIds =
				vote.options().stream().map(option -> option.optionId()).toList();
		voteParticipationService.participate(
				vote.voteId(), first.getId(), new VoteParticipationRequest(optionIds));

		MyVoteResponse response =
				voteParticipationService.findMyParticipation(vote.voteId(), first.getId());

		assertThat(response.memberId()).isEqualTo(first.getId());
		assertThat(response.selectedOptionIds()).containsExactlyInAnyOrderElementsOf(optionIds);
		assertThat(response.participatedAt()).isNotNull();
	}

	@Test
	@DisplayName("집계 결과는 득표 수 내림차순으로 정렬되고 최다 득표 선택지를 표시한다")
	void findResult() {
		VoteResponse vote = createVote(VoteType.SINGLE);
		Long firstOptionId = vote.options().get(0).optionId();
		Long secondOptionId = vote.options().get(1).optionId();
		voteParticipationService.participate(
				vote.voteId(), first.getId(), new VoteParticipationRequest(List.of(secondOptionId)));
		voteParticipationService.participate(
				vote.voteId(), second.getId(), new VoteParticipationRequest(List.of(secondOptionId)));
		voteParticipationService.participate(
				vote.voteId(), creator.getId(), new VoteParticipationRequest(List.of(firstOptionId)));

		VoteResultResponse result = voteParticipationService.findResult(vote.voteId());

		assertThat(result.participantCount()).isEqualTo(3);
		assertThat(result.totalSelectionCount()).isEqualTo(3);
		assertThat(result.results().get(0).optionId()).isEqualTo(secondOptionId);
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
						type,
						LocalDateTime.now().plusDays(1),
						List.of(
								new VoteOptionCreateRequest("돼지국밥", null),
								new VoteOptionCreateRequest("밀면", null))));
	}
}
