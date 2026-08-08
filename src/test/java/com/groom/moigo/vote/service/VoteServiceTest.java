package com.groom.moigo.vote.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.groom.moigo.auth.entity.Member;
import com.groom.moigo.auth.repository.MemberRepository;
import com.groom.moigo.place.entity.Place;
import com.groom.moigo.place.repository.PlaceRepository;
import com.groom.moigo.plan.entity.Plan;
import com.groom.moigo.plan.repository.PlanRepository;
import com.groom.moigo.vote.dto.request.VoteCreateRequest;
import com.groom.moigo.vote.dto.request.VoteOptionCreateRequest;
import com.groom.moigo.vote.dto.request.VoteOptionUpdateRequest;
import com.groom.moigo.vote.dto.request.VoteParticipationRequest;
import com.groom.moigo.vote.dto.request.VoteUpdateRequest;
import com.groom.moigo.vote.dto.response.VoteOptionResponse;
import com.groom.moigo.vote.dto.response.VoteResponse;
import com.groom.moigo.vote.entity.VoteStatus;
import com.groom.moigo.vote.entity.VoteType;
import com.groom.moigo.vote.exception.VoteErrorCode;
import com.groom.moigo.vote.exception.VoteException;
import com.groom.moigo.vote.repository.VoteRepository;
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
class VoteServiceTest {

	@Autowired private VoteService voteService;
	@Autowired private VoteParticipationService voteParticipationService;
	@Autowired private VoteRepository voteRepository;
	@Autowired private PlanRepository planRepository;
	@Autowired private MemberRepository memberRepository;
	@Autowired private PlaceRepository placeRepository;

	private Plan plan;
	private Member creator;
	private Member participant;
	private Place place;

	@BeforeEach
	void setUp() {
		plan = planRepository.save(new Plan("제주도 3박 4일"));
		creator = memberRepository.save(new Member("creator@moigo.com", "생성자", null));
		participant = memberRepository.save(new Member("member@moigo.com", "참여자", null));
		place = placeRepository.save(new Place("kakao-1", "성산일출봉"));
	}

	@Test
	@DisplayName("투표를 생성하면 선택지가 함께 저장되고 진행 상태가 된다")
	void createVote() {
		VoteResponse response = voteService.create(plan.getId(), creator.getId(), createRequest());

		assertThat(response.id()).isNotNull();
		assertThat(response.planId()).isEqualTo(String.valueOf(plan.getId()));
		assertThat(response.creatorId()).isEqualTo(String.valueOf(creator.getId()));
		assertThat(response.status()).isEqualTo(VoteStatus.OPEN);
		assertThat(response.options()).hasSize(2);
		assertThat(response.options().get(0).placeId()).isEqualTo(String.valueOf(place.getId()));
		assertThat(response.options().get(1).placeId()).isNull();
		assertThat(response.participantCount()).isZero();
		assertThat(response.myOptionId()).isNull();
		assertThat(response.resultSummary()).isNull();
	}

	@Test
	@DisplayName("선택지에 장소 이름·주소·이모지가 그대로 저장된다")
	void createVoteKeepsPlaceSnapshot() {
		VoteResponse response = voteService.create(plan.getId(), creator.getId(), createRequest());

		VoteOptionResponse first = response.options().get(0);
		assertThat(first.placeName()).isEqualTo("성산일출봉");
		assertThat(first.placeAddress()).isEqualTo("제주 서귀포시 성산읍");
		assertThat(first.emoji()).isEqualTo("🌅");
		assertThat(first.voteId()).isEqualTo(response.id());
	}

	@Test
	@DisplayName("이모지를 보내지 않으면 기본 핀 이모지가 채워진다")
	void createVoteFillsDefaultEmoji() {
		VoteCreateRequest request =
				new VoteCreateRequest(
						"어디로 갈까요",
						null,
						Instant.now().plus(1, ChronoUnit.DAYS),
						null,
						List.of(
								new VoteOptionCreateRequest("성산일출봉", null, null, null),
								new VoteOptionCreateRequest("협재해수욕장", null, "  ", null)));

		VoteResponse response = voteService.create(plan.getId(), creator.getId(), request);

		assertThat(response.options()).extracting(VoteOptionResponse::emoji).containsOnly("📍");
	}

	@Test
	@DisplayName("투표 방식을 생략하면 단일 선택으로 만들어진다")
	void createVoteDefaultsToSingleChoice() {
		VoteResponse response =
				voteService.create(
						plan.getId(),
						creator.getId(),
						new VoteCreateRequest(
								"어디로 갈까요",
								null,
								Instant.now().plus(1, ChronoUnit.DAYS),
								null,
								List.of(
										new VoteOptionCreateRequest("성산일출봉", null, null, null),
										new VoteOptionCreateRequest("협재해수욕장", null, null, null))));

		assertThat(response.type()).isEqualTo(VoteType.SINGLE);
	}

	@Test
	@DisplayName("존재하지 않는 계획에는 투표를 만들 수 없다")
	void createVoteWithUnknownPlan() {
		assertThatThrownBy(() -> voteService.create(999_999L, creator.getId(), createRequest()))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.PLAN_NOT_FOUND);
	}

	@Test
	@DisplayName("마감 일시가 과거면 투표를 만들 수 없다")
	void createVoteWithPastDeadline() {
		VoteCreateRequest request =
				new VoteCreateRequest(
						"어디로 갈까요",
						null,
						Instant.now().minus(1, ChronoUnit.MINUTES),
						VoteType.SINGLE,
						List.of(
								new VoteOptionCreateRequest("성산일출봉", null, null, null),
								new VoteOptionCreateRequest("협재해수욕장", null, null, null)));

		assertThatThrownBy(() -> voteService.create(plan.getId(), creator.getId(), request))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.INVALID_DEADLINE);
	}

	@Test
	@DisplayName("계획의 투표 목록에 선택지와 득표 수, 내 선택이 함께 담긴다")
	void findAllByPlan() {
		VoteResponse vote = voteService.create(plan.getId(), creator.getId(), createRequest());
		String optionId = vote.options().get(0).id();
		voteParticipationService.participate(
				plan.getId(),
				id(vote.id()),
				participant.getId(),
				new VoteParticipationRequest(id(optionId), null));

		List<VoteResponse> votes = voteService.findAllByPlan(plan.getId(), participant.getId());

		assertThat(votes).hasSize(1);
		VoteResponse found = votes.get(0);
		assertThat(found.options()).hasSize(2);
		assertThat(found.participantCount()).isEqualTo(1);
		assertThat(found.myOptionId()).isEqualTo(optionId);
		assertThat(found.options().get(0).voteCount()).isEqualTo(1);
		assertThat(found.options().get(0).selectedByMe()).isTrue();
	}

	@Test
	@DisplayName("회원을 알 수 없는 목록 조회에서는 내 선택이 비어 있다")
	void findAllByPlanWithoutMember() {
		voteService.create(plan.getId(), creator.getId(), createRequest());

		List<VoteResponse> votes = voteService.findAllByPlan(plan.getId(), null);

		assertThat(votes).hasSize(1);
		assertThat(votes.get(0).myOptionId()).isNull();
		assertThat(votes.get(0).myOptionIds()).isEmpty();
	}

	@Test
	@DisplayName("다른 계획의 투표는 조회할 수 없다")
	void findByIdOfAnotherPlan() {
		VoteResponse vote = voteService.create(plan.getId(), creator.getId(), createRequest());
		Plan other = planRepository.save(new Plan("부산 당일치기"));

		assertThatThrownBy(() -> voteService.findById(other.getId(), id(vote.id()), creator.getId()))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.VOTE_NOT_IN_PLAN);
	}

	@Test
	@DisplayName("생성자가 아니면 투표를 수정할 수 없다")
	void updateByNonCreator() {
		VoteResponse vote = voteService.create(plan.getId(), creator.getId(), createRequest());
		VoteUpdateRequest request = new VoteUpdateRequest("변경", null, null);

		assertThatThrownBy(
						() -> voteService.update(plan.getId(), id(vote.id()), participant.getId(), request))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.NOT_VOTE_CREATOR);
	}

	@Test
	@DisplayName("투표를 즉시 마감하면 결과 요약이 채워지고 더 이상 수정할 수 없다")
	void closeVote() {
		VoteResponse vote = voteService.create(plan.getId(), creator.getId(), createRequest());
		voteParticipationService.participate(
				plan.getId(),
				id(vote.id()),
				participant.getId(),
				new VoteParticipationRequest(id(vote.options().get(0).id()), null));

		VoteResponse closed = voteService.close(plan.getId(), id(vote.id()), creator.getId());

		assertThat(closed.status()).isEqualTo(VoteStatus.CLOSED);
		assertThat(closed.resultSummary()).isEqualTo("성산일출봉 1표 · 확정");

		VoteUpdateRequest request = new VoteUpdateRequest("변경", null, null);
		assertThatThrownBy(
						() -> voteService.update(plan.getId(), id(vote.id()), creator.getId(), request))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.VOTE_ALREADY_CLOSED);
	}

	@Test
	@DisplayName("아무도 투표하지 않고 마감하면 결과 요약이 그 사실을 알려 준다")
	void closeVoteWithoutParticipation() {
		VoteResponse vote = voteService.create(plan.getId(), creator.getId(), createRequest());

		VoteResponse closed = voteService.close(plan.getId(), id(vote.id()), creator.getId());

		assertThat(closed.resultSummary()).isEqualTo("투표 없이 마감되었어요");
	}

	@Test
	@DisplayName("동점으로 마감하면 결과 요약에 동률이 표시된다")
	void closeVoteWithTie() {
		VoteResponse vote = voteService.create(plan.getId(), creator.getId(), createRequest());
		voteParticipationService.participate(
				plan.getId(),
				id(vote.id()),
				creator.getId(),
				new VoteParticipationRequest(id(vote.options().get(0).id()), null));
		voteParticipationService.participate(
				plan.getId(),
				id(vote.id()),
				participant.getId(),
				new VoteParticipationRequest(id(vote.options().get(1).id()), null));

		VoteResponse closed = voteService.close(plan.getId(), id(vote.id()), creator.getId());

		assertThat(closed.resultSummary()).isEqualTo("성산일출봉 외 1곳 1표 동률");
	}

	@Test
	@DisplayName("마감 일시가 지난 투표는 조회 시 마감 상태로 동기화된다")
	void syncStatusAfterDeadline() {
		VoteResponse vote = voteService.create(plan.getId(), creator.getId(), createRequest());

		voteRepository
				.findById(id(vote.id()))
				.orElseThrow()
				.update(null, null, Instant.now().minus(1, ChronoUnit.MINUTES));

		VoteResponse found = voteService.findById(plan.getId(), id(vote.id()), creator.getId());
		assertThat(found.status()).isEqualTo(VoteStatus.CLOSED);
	}

	@Test
	@DisplayName("선택지를 추가하면 목록에 반영된다")
	void addOption() {
		VoteResponse vote = voteService.create(plan.getId(), creator.getId(), createRequest());

		VoteOptionResponse added =
				voteService.addOption(
						plan.getId(),
						id(vote.id()),
						creator.getId(),
						new VoteOptionCreateRequest("우도", "제주 우도면", "⛴️", null));

		assertThat(added.id()).isNotNull();
		assertThat(added.placeName()).isEqualTo("우도");
		assertThat(voteService.findById(plan.getId(), id(vote.id()), creator.getId()).options())
				.hasSize(3);
	}

	@Test
	@DisplayName("선택지 수정 시 placeId를 비우면 장소 연결이 해제된다")
	void updateOptionDetachesPlace() {
		VoteResponse vote = voteService.create(plan.getId(), creator.getId(), createRequest());
		String optionId = vote.options().get(0).id();

		VoteOptionResponse updated =
				voteService.updateOption(
						plan.getId(),
						id(vote.id()),
						id(optionId),
						creator.getId(),
						new VoteOptionUpdateRequest("우도", "제주 우도면", "⛴️", null));

		assertThat(updated.placeName()).isEqualTo("우도");
		assertThat(updated.placeAddress()).isEqualTo("제주 우도면");
		assertThat(updated.placeId()).isNull();
	}

	@Test
	@DisplayName("선택지가 2개뿐이면 삭제할 수 없다")
	void deleteOptionBelowMinimum() {
		VoteResponse vote = voteService.create(plan.getId(), creator.getId(), createRequest());
		String optionId = vote.options().get(0).id();

		assertThatThrownBy(
						() ->
								voteService.deleteOption(
										plan.getId(), id(vote.id()), id(optionId), creator.getId()))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.OPTION_BELOW_MINIMUM);
	}

	@Test
	@DisplayName("선택지를 삭제하면 그 선택지에 걸린 참여 기록도 사라진다")
	void deleteOptionRemovesParticipation() {
		VoteResponse vote = voteService.create(plan.getId(), creator.getId(), createRequest());
		voteService.addOption(
				plan.getId(),
				id(vote.id()),
				creator.getId(),
				new VoteOptionCreateRequest("우도", null, null, null));
		String optionId = vote.options().get(0).id();
		voteParticipationService.participate(
				plan.getId(),
				id(vote.id()),
				participant.getId(),
				new VoteParticipationRequest(id(optionId), null));

		voteService.deleteOption(plan.getId(), id(vote.id()), id(optionId), creator.getId());

		VoteResponse found = voteService.findById(plan.getId(), id(vote.id()), participant.getId());
		assertThat(found.options()).hasSize(2);
		assertThat(found.participantCount()).isZero();
	}

	@Test
	@DisplayName("다른 투표의 선택지 ID로는 수정할 수 없다")
	void updateOptionOfAnotherVote() {
		VoteResponse first = voteService.create(plan.getId(), creator.getId(), createRequest());
		VoteResponse second = voteService.create(plan.getId(), creator.getId(), createRequest());
		String foreignOptionId = second.options().get(0).id();

		assertThatThrownBy(
						() ->
								voteService.updateOption(
										plan.getId(),
										id(first.id()),
										id(foreignOptionId),
										creator.getId(),
										new VoteOptionUpdateRequest("우도", null, null, null)))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.OPTION_NOT_IN_VOTE);
	}

	@Test
	@DisplayName("투표를 삭제하면 선택지와 참여 기록도 함께 사라진다")
	void deleteVote() {
		VoteResponse vote = voteService.create(plan.getId(), creator.getId(), createRequest());
		voteParticipationService.participate(
				plan.getId(),
				id(vote.id()),
				participant.getId(),
				new VoteParticipationRequest(id(vote.options().get(0).id()), null));

		voteService.delete(plan.getId(), id(vote.id()), creator.getId());

		assertThat(voteRepository.findById(id(vote.id()))).isEmpty();
	}

	private VoteCreateRequest createRequest() {
		return new VoteCreateRequest(
				"첫날 어디 갈까요",
				"오전 일정 후보입니다",
				Instant.now().plus(1, ChronoUnit.DAYS),
				VoteType.SINGLE,
				List.of(
						new VoteOptionCreateRequest("성산일출봉", "제주 서귀포시 성산읍", "🌅", place.getId()),
						new VoteOptionCreateRequest("협재해수욕장", "제주 한림읍", "🏖️", null)));
	}

	private static long id(String value) {
		return Long.parseLong(value);
	}
}
