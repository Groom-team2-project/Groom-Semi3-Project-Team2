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
import com.groom.moigo.vote.dto.response.VoteSummaryResponse;
import com.groom.moigo.vote.entity.VoteStatus;
import com.groom.moigo.vote.entity.VoteType;
import com.groom.moigo.vote.exception.VoteErrorCode;
import com.groom.moigo.vote.exception.VoteException;
import com.groom.moigo.vote.repository.VoteRepository;
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

		assertThat(response.voteId()).isNotNull();
		assertThat(response.planId()).isEqualTo(plan.getId());
		assertThat(response.creatorId()).isEqualTo(creator.getId());
		assertThat(response.status()).isEqualTo(VoteStatus.OPEN);
		assertThat(response.options()).hasSize(2);
		assertThat(response.options().get(0).placeId()).isEqualTo(place.getId());
		assertThat(response.options().get(1).placeId()).isNull();
		assertThat(response.participantCount()).isZero();
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
	@DisplayName("종료 일시가 과거면 투표를 만들 수 없다")
	void createVoteWithPastClosesAt() {
		VoteCreateRequest request =
				new VoteCreateRequest(
						"어디로 갈까요",
						null,
						VoteType.SINGLE,
						LocalDateTime.now().minusMinutes(1),
						List.of(
								new VoteOptionCreateRequest("성산일출봉", null),
								new VoteOptionCreateRequest("협재해수욕장", null)));

		assertThatThrownBy(() -> voteService.create(plan.getId(), creator.getId(), request))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.INVALID_CLOSES_AT);
	}

	@Test
	@DisplayName("계획의 투표 목록에 선택지 수와 참여 인원이 담긴다")
	void findAllByPlan() {
		VoteResponse vote = voteService.create(plan.getId(), creator.getId(), createRequest());
		voteParticipationService.participate(
				vote.voteId(),
				participant.getId(),
				new VoteParticipationRequest(List.of(vote.options().get(0).optionId())));

		List<VoteSummaryResponse> summaries = voteService.findAllByPlan(plan.getId());

		assertThat(summaries).hasSize(1);
		assertThat(summaries.get(0).optionCount()).isEqualTo(2);
		assertThat(summaries.get(0).participantCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("생성자가 아니면 투표를 수정할 수 없다")
	void updateByNonCreator() {
		VoteResponse vote = voteService.create(plan.getId(), creator.getId(), createRequest());
		VoteUpdateRequest request = new VoteUpdateRequest("변경", null, null);

		assertThatThrownBy(() -> voteService.update(vote.voteId(), participant.getId(), request))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.NOT_VOTE_CREATOR);
	}

	@Test
	@DisplayName("투표를 즉시 종료하면 상태가 종료로 바뀌고 더 이상 수정할 수 없다")
	void closeVote() {
		VoteResponse vote = voteService.create(plan.getId(), creator.getId(), createRequest());

		VoteResponse closed = voteService.close(vote.voteId(), creator.getId());
		assertThat(closed.status()).isEqualTo(VoteStatus.CLOSED);

		VoteUpdateRequest request = new VoteUpdateRequest("변경", null, null);
		assertThatThrownBy(() -> voteService.update(vote.voteId(), creator.getId(), request))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.VOTE_ALREADY_CLOSED);
	}

	@Test
	@DisplayName("종료 일시가 지난 투표는 조회 시 종료 상태로 동기화된다")
	void syncStatusAfterClosesAt() {
		VoteResponse vote =
				voteService.create(
						plan.getId(),
						creator.getId(),
						new VoteCreateRequest(
								"어디로 갈까요",
								null,
								VoteType.SINGLE,
								LocalDateTime.now().plusSeconds(1),
								List.of(
										new VoteOptionCreateRequest("성산일출봉", null),
										new VoteOptionCreateRequest("협재해수욕장", null))));

		voteRepository
				.findById(vote.voteId())
				.orElseThrow()
				.update(null, null, LocalDateTime.now().minusMinutes(1));

		VoteResponse found = voteService.findById(vote.voteId(), creator.getId());
		assertThat(found.status()).isEqualTo(VoteStatus.CLOSED);
	}

	@Test
	@DisplayName("선택지를 추가하면 목록에 반영된다")
	void addOption() {
		VoteResponse vote = voteService.create(plan.getId(), creator.getId(), createRequest());

		VoteOptionResponse added =
				voteService.addOption(
						vote.voteId(), creator.getId(), new VoteOptionCreateRequest("우도", null));

		assertThat(added.optionId()).isNotNull();
		assertThat(voteService.findById(vote.voteId(), creator.getId()).options()).hasSize(3);
	}

	@Test
	@DisplayName("선택지 수정 시 placeId를 비우면 장소 연결이 해제된다")
	void updateOptionDetachesPlace() {
		VoteResponse vote = voteService.create(plan.getId(), creator.getId(), createRequest());
		Long optionId = vote.options().get(0).optionId();

		VoteOptionResponse updated =
				voteService.updateOption(
						vote.voteId(), optionId, creator.getId(), new VoteOptionUpdateRequest("우도", null));

		assertThat(updated.content()).isEqualTo("우도");
		assertThat(updated.placeId()).isNull();
	}

	@Test
	@DisplayName("선택지가 2개뿐이면 삭제할 수 없다")
	void deleteOptionBelowMinimum() {
		VoteResponse vote = voteService.create(plan.getId(), creator.getId(), createRequest());
		Long optionId = vote.options().get(0).optionId();

		assertThatThrownBy(() -> voteService.deleteOption(vote.voteId(), optionId, creator.getId()))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.OPTION_BELOW_MINIMUM);
	}

	@Test
	@DisplayName("선택지를 삭제하면 그 선택지에 걸린 참여 기록도 사라진다")
	void deleteOptionRemovesParticipation() {
		VoteResponse vote = voteService.create(plan.getId(), creator.getId(), createRequest());
		voteService.addOption(vote.voteId(), creator.getId(), new VoteOptionCreateRequest("우도", null));
		Long optionId = vote.options().get(0).optionId();
		voteParticipationService.participate(
				vote.voteId(), participant.getId(), new VoteParticipationRequest(List.of(optionId)));

		voteService.deleteOption(vote.voteId(), optionId, creator.getId());

		VoteResponse found = voteService.findById(vote.voteId(), participant.getId());
		assertThat(found.options()).hasSize(2);
		assertThat(found.participantCount()).isZero();
	}

	@Test
	@DisplayName("다른 투표의 선택지 ID로는 수정할 수 없다")
	void updateOptionOfAnotherVote() {
		VoteResponse first = voteService.create(plan.getId(), creator.getId(), createRequest());
		VoteResponse second = voteService.create(plan.getId(), creator.getId(), createRequest());
		Long foreignOptionId = second.options().get(0).optionId();

		assertThatThrownBy(
						() ->
								voteService.updateOption(
										first.voteId(),
										foreignOptionId,
										creator.getId(),
										new VoteOptionUpdateRequest("우도", null)))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.OPTION_NOT_IN_VOTE);
	}

	@Test
	@DisplayName("투표를 삭제하면 선택지와 참여 기록도 함께 사라진다")
	void deleteVote() {
		VoteResponse vote = voteService.create(plan.getId(), creator.getId(), createRequest());
		voteParticipationService.participate(
				vote.voteId(),
				participant.getId(),
				new VoteParticipationRequest(List.of(vote.options().get(0).optionId())));

		voteService.delete(vote.voteId(), creator.getId());

		assertThat(voteRepository.findById(vote.voteId())).isEmpty();
	}

	private VoteCreateRequest createRequest() {
		return new VoteCreateRequest(
				"첫날 어디 갈까요",
				"오전 일정 후보입니다",
				VoteType.SINGLE,
				LocalDateTime.now().plusDays(1),
				List.of(
						new VoteOptionCreateRequest("성산일출봉", place.getId()),
						new VoteOptionCreateRequest("협재해수욕장", null)));
	}
}
