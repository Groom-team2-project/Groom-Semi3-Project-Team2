package com.groom.moigo.domain.vote.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.groom.moigo.domain.activity.entity.ActivityActionType;
import com.groom.moigo.domain.activity.repository.ActivityLogRepository;
import com.groom.moigo.domain.plan.entity.MemberRole;
import com.groom.moigo.domain.vote.dto.request.VoteCreateRequest;
import com.groom.moigo.domain.vote.dto.request.VoteOptionCreateRequest;
import com.groom.moigo.domain.vote.dto.request.VoteOptionUpdateRequest;
import com.groom.moigo.domain.vote.dto.request.VoteParticipationRequest;
import com.groom.moigo.domain.vote.dto.request.VoteUpdateRequest;
import com.groom.moigo.domain.vote.dto.response.VoteOptionResponse;
import com.groom.moigo.domain.vote.dto.response.VoteResponse;
import com.groom.moigo.domain.vote.entity.VoteStatus;
import com.groom.moigo.domain.vote.entity.VoteType;
import com.groom.moigo.domain.vote.exception.VoteErrorCode;
import com.groom.moigo.domain.vote.exception.VoteException;
import com.groom.moigo.domain.vote.repository.VoteRepository;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class VoteServiceTest {

	@Autowired private VoteService voteService;
	@Autowired private VoteParticipationService voteParticipationService;
	@Autowired private VoteRepository voteRepository;
	@Autowired private ActivityLogRepository activityLogRepository;
	@Autowired private VoteTestFixture fixture;

	private Long planId;
	private Long creatorId;
	private Long participantId;
	private Long placeId;

	@BeforeEach
	void setUp() {
		creatorId = fixture.createUser("생성자");
		participantId = fixture.createUser("참여자");
		planId = fixture.createPlan(creatorId, "제주도 3박 4일");
		fixture.join(planId, participantId, MemberRole.EDITOR);
		placeId = fixture.createPlace("성산일출봉");
	}

	@Test
	@DisplayName("계획 멤버가 아니면 투표를 조회할 수 없다")
	void findAllByPlanAsNonMember() {
		Long outsiderId = fixture.createUser("외부인");

		assertThatThrownBy(() -> voteService.findAllByPlan(planId, outsiderId))
				.isInstanceOf(BusinessException.class)
				.extracting(exception -> ((BusinessException) exception).getErrorCode())
				.isEqualTo(ErrorCode.PLAN_ACCESS_DENIED);
	}

	@Test
	@DisplayName("계획에서 나간 멤버는 투표를 조회할 수 없다")
	void findAllByPlanAsLeftMember() {
		Long leftId = fixture.createUser("나간사람");
		fixture.leave(planId, leftId, MemberRole.EDITOR);

		assertThatThrownBy(() -> voteService.findAllByPlan(planId, leftId))
				.isInstanceOf(BusinessException.class)
				.extracting(exception -> ((BusinessException) exception).getErrorCode())
				.isEqualTo(ErrorCode.PLAN_ACCESS_DENIED);
	}

	@Test
	@DisplayName("VIEWER는 투표를 만들 수 없다")
	void createVoteAsViewer() {
		Long viewerId = fixture.createUser("뷰어");
		fixture.join(planId, viewerId, MemberRole.VIEWER);

		assertThatThrownBy(() -> voteService.create(planId, viewerId, createRequest()))
				.isInstanceOf(BusinessException.class)
				.extracting(exception -> ((BusinessException) exception).getErrorCode())
				.isEqualTo(ErrorCode.PLAN_UPDATE_FORBIDDEN);
	}

	@Test
	@DisplayName("투표를 생성하면 선택지가 함께 저장되고 진행 상태가 된다")
	void createVote() {
		VoteResponse response = voteService.create(planId, creatorId, createRequest());

		assertThat(response.id()).isNotNull();
		assertThat(response.planId()).isEqualTo(String.valueOf(planId));
		assertThat(response.creatorId()).isEqualTo(String.valueOf(creatorId));
		assertThat(response.status()).isEqualTo(VoteStatus.OPEN);
		assertThat(response.options()).hasSize(2);
		assertThat(response.options().get(0).placeId()).isEqualTo(String.valueOf(placeId));
		assertThat(response.options().get(1).placeId()).isNull();
		assertThat(response.participantCount()).isZero();
		assertThat(response.myOptionId()).isNull();
		assertThat(response.resultSummary()).isNull();
	}

	@Test
	@DisplayName("투표를 생성하면 활동 이력이 남는다")
	void createVoteRecordsActivity() {
		VoteResponse response = voteService.create(planId, creatorId, createRequest());

		assertThat(activityLogRepository.findAll())
				.singleElement()
				.satisfies(
						log -> {
							assertThat(log.getPlanId()).isEqualTo(planId);
							assertThat(log.getUserId()).isEqualTo(creatorId);
							assertThat(log.getActionType()).isEqualTo(ActivityActionType.VOTE_CREATED);
							assertThat(log.getTargetId()).isEqualTo(Long.valueOf(response.id()));
							assertThat(log.getSummary()).isEqualTo("'첫날 어디 갈까요' 투표를 시작했어요");
						});
	}

	@Test
	@DisplayName("선택지에 장소 이름·주소·이모지가 그대로 저장된다")
	void createVoteKeepsPlaceSnapshot() {
		VoteResponse response = voteService.create(planId, creatorId, createRequest());

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
						null,
						List.of(
								new VoteOptionCreateRequest("성산일출봉", null, null, null),
								new VoteOptionCreateRequest("협재해수욕장", null, "  ", null)));

		VoteResponse response = voteService.create(planId, creatorId, request);

		assertThat(response.options()).extracting(VoteOptionResponse::emoji).containsOnly("📍");
	}

	@Test
	@DisplayName("투표 방식을 생략하면 단일 선택으로 만들어진다")
	void createVoteDefaultsToSingleChoice() {
		VoteResponse response = voteService.create(planId, creatorId, minimalRequest());

		assertThat(response.type()).isEqualTo(VoteType.SINGLE);
	}

	@Test
	@DisplayName("일정을 지정해 투표를 만들면 linkedScheduleId로 내려온다")
	void createVoteLinkedToSchedule() {
		Long scheduleId = fixture.createSchedule(planId, "둘째날 저녁");
		VoteCreateRequest request =
				new VoteCreateRequest(
						"둘째날 저녁 뭐 먹지?",
						null,
						Instant.now().plus(1, ChronoUnit.DAYS),
						null,
						scheduleId,
						List.of(
								new VoteOptionCreateRequest("연돈", null, null, null),
								new VoteOptionCreateRequest("제주바다", null, null, null)));

		VoteResponse response = voteService.create(planId, creatorId, request);

		assertThat(response.linkedScheduleId()).isEqualTo(String.valueOf(scheduleId));
	}

	@Test
	@DisplayName("마감 일시를 보내지 않으면 투표를 만들 수 없다")
	void createVoteWithoutDeadline() {
		VoteCreateRequest request =
				new VoteCreateRequest(
						"어디로 갈까요",
						null,
						null,
						VoteType.SINGLE,
						null,
						List.of(
								new VoteOptionCreateRequest("성산일출봉", null, null, null),
								new VoteOptionCreateRequest("협재해수욕장", null, null, null)));

		assertThatThrownBy(() -> voteService.create(planId, creatorId, request))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.INVALID_DEADLINE);
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
						null,
						List.of(
								new VoteOptionCreateRequest("성산일출봉", null, null, null),
								new VoteOptionCreateRequest("협재해수욕장", null, null, null)));

		assertThatThrownBy(() -> voteService.create(planId, creatorId, request))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.INVALID_DEADLINE);
	}

	@Test
	@DisplayName("계획의 투표 목록에 선택지와 득표 수, 내 선택이 함께 담긴다")
	void findAllByPlan() {
		VoteResponse vote = voteService.create(planId, creatorId, createRequest());
		String optionId = vote.options().get(0).id();
		voteParticipationService.participate(
				planId, id(vote.id()), participantId, new VoteParticipationRequest(id(optionId), null));

		List<VoteResponse> votes = voteService.findAllByPlan(planId, participantId);

		assertThat(votes).hasSize(1);
		VoteResponse found = votes.get(0);
		assertThat(found.options()).hasSize(2);
		assertThat(found.participantCount()).isEqualTo(1);
		assertThat(found.myOptionId()).isEqualTo(optionId);
		assertThat(found.options().get(0).voteCount()).isEqualTo(1);
		assertThat(found.options().get(0).selectedByMe()).isTrue();
	}

	@Test
	@DisplayName("투표하지 않은 회원의 목록 조회에서는 내 선택이 비어 있다")
	void findAllByPlanWithoutParticipation() {
		voteService.create(planId, creatorId, createRequest());

		List<VoteResponse> votes = voteService.findAllByPlan(planId, participantId);

		assertThat(votes).hasSize(1);
		assertThat(votes.get(0).myOptionId()).isNull();
		assertThat(votes.get(0).myOptionIds()).isEmpty();
	}

	@Test
	@DisplayName("다른 계획의 투표는 조회할 수 없다")
	void findByIdOfAnotherPlan() {
		VoteResponse vote = voteService.create(planId, creatorId, createRequest());
		Long otherPlanId = fixture.createPlan(creatorId, "부산 당일치기");

		assertThatThrownBy(() -> voteService.findById(otherPlanId, id(vote.id()), creatorId))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.VOTE_NOT_IN_PLAN);
	}

	@Test
	@DisplayName("생성자가 아니면 투표를 수정할 수 없다")
	void updateByNonCreator() {
		VoteResponse vote = voteService.create(planId, creatorId, createRequest());
		VoteUpdateRequest request = new VoteUpdateRequest("변경", null, null, null);

		assertThatThrownBy(() -> voteService.update(planId, id(vote.id()), participantId, request))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.NOT_VOTE_CREATOR);
	}

	@Test
	@DisplayName("투표를 즉시 마감하면 결과 요약이 채워지고 활동 이력이 남는다")
	void closeVote() {
		VoteResponse vote = voteService.create(planId, creatorId, createRequest());
		voteParticipationService.participate(
				planId,
				id(vote.id()),
				participantId,
				new VoteParticipationRequest(id(vote.options().get(0).id()), null));

		VoteResponse closed = voteService.close(planId, id(vote.id()), creatorId);

		assertThat(closed.status()).isEqualTo(VoteStatus.CLOSED);
		assertThat(closed.resultSummary()).isEqualTo("성산일출봉 1표 · 확정");
		assertThat(activityLogRepository.findAll())
				.extracting(log -> log.getActionType())
				.contains(ActivityActionType.VOTE_CLOSED);

		VoteUpdateRequest request = new VoteUpdateRequest("변경", null, null, null);
		assertThatThrownBy(() -> voteService.update(planId, id(vote.id()), creatorId, request))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.VOTE_ALREADY_CLOSED);
	}

	@Test
	@DisplayName("아무도 투표하지 않고 마감하면 결과 요약이 그 사실을 알려 준다")
	void closeVoteWithoutParticipation() {
		VoteResponse vote = voteService.create(planId, creatorId, createRequest());

		VoteResponse closed = voteService.close(planId, id(vote.id()), creatorId);

		assertThat(closed.resultSummary()).isEqualTo("투표 없이 마감되었어요");
	}

	@Test
	@DisplayName("동점으로 마감하면 결과 요약에 동률이 표시된다")
	void closeVoteWithTie() {
		VoteResponse vote = voteService.create(planId, creatorId, createRequest());
		voteParticipationService.participate(
				planId,
				id(vote.id()),
				creatorId,
				new VoteParticipationRequest(id(vote.options().get(0).id()), null));
		voteParticipationService.participate(
				planId,
				id(vote.id()),
				participantId,
				new VoteParticipationRequest(id(vote.options().get(1).id()), null));

		VoteResponse closed = voteService.close(planId, id(vote.id()), creatorId);

		assertThat(closed.resultSummary()).isEqualTo("성산일출봉 외 1곳 1표 동률");
	}

	@Test
	@DisplayName("마감 일시가 지난 투표는 조회 시 마감 상태로 동기화된다")
	void syncStatusAfterDeadline() {
		VoteResponse vote = voteService.create(planId, creatorId, createRequest());

		voteRepository
				.findById(id(vote.id()))
				.orElseThrow()
				.update(null, null, Instant.now().minus(1, ChronoUnit.MINUTES));

		VoteResponse found = voteService.findById(planId, id(vote.id()), creatorId);
		assertThat(found.status()).isEqualTo(VoteStatus.CLOSED);
	}

	@Test
	@DisplayName("선택지를 추가하면 목록에 반영된다")
	void addOption() {
		VoteResponse vote = voteService.create(planId, creatorId, createRequest());

		VoteOptionResponse added =
				voteService.addOption(
						planId,
						id(vote.id()),
						creatorId,
						new VoteOptionCreateRequest("우도", "제주 우도면", "⛴️", null));

		assertThat(added.id()).isNotNull();
		assertThat(added.placeName()).isEqualTo("우도");
		assertThat(voteService.findById(planId, id(vote.id()), creatorId).options()).hasSize(3);
	}

	@Test
	@DisplayName("선택지 수정 시 placeId를 생략하면 기존 장소 연결이 유지된다")
	void updateOptionKeepsPlaceWhenPlaceIdOmitted() {
		VoteResponse vote = voteService.create(planId, creatorId, createRequest());
		String optionId = vote.options().get(0).id();

		VoteOptionResponse updated =
				voteService.updateOption(
						planId,
						id(vote.id()),
						id(optionId),
						creatorId,
						new VoteOptionUpdateRequest("우도", "제주 우도면", "⛴️", null, false));

		assertThat(updated.placeName()).isEqualTo("우도");
		assertThat(updated.placeAddress()).isEqualTo("제주 우도면");
		assertThat(updated.placeId()).isEqualTo(String.valueOf(placeId));
	}

	@Test
	@DisplayName("선택지 수정 시 clearPlace를 보내야 장소 연결이 해제된다")
	void updateOptionDetachesPlaceOnlyWithClearPlace() {
		VoteResponse vote = voteService.create(planId, creatorId, createRequest());
		String optionId = vote.options().get(0).id();

		VoteOptionResponse updated =
				voteService.updateOption(
						planId,
						id(vote.id()),
						id(optionId),
						creatorId,
						new VoteOptionUpdateRequest("우도", null, null, null, true));

		assertThat(updated.placeId()).isNull();
	}

	@Test
	@DisplayName("선택지 수정으로 다른 장소를 연결할 수 있다")
	void updateOptionAttachesAnotherPlace() {
		VoteResponse vote = voteService.create(planId, creatorId, createRequest());
		String optionId = vote.options().get(1).id();
		Long anotherPlaceId = fixture.createPlace("우도");

		VoteOptionResponse updated =
				voteService.updateOption(
						planId,
						id(vote.id()),
						id(optionId),
						creatorId,
						new VoteOptionUpdateRequest("우도", null, null, anotherPlaceId, false));

		assertThat(updated.placeId()).isEqualTo(String.valueOf(anotherPlaceId));
	}

	@Test
	@DisplayName("다른 계획의 일정에는 투표를 연결할 수 없다")
	void createVoteLinkedToScheduleOfAnotherPlan() {
		Long otherPlanId = fixture.createPlan(creatorId, "부산 당일치기");
		Long foreignScheduleId = fixture.createSchedule(otherPlanId, "남의 계획 일정");
		VoteCreateRequest request =
				new VoteCreateRequest(
						"둘째날 저녁 뭐 먹지?",
						null,
						Instant.now().plus(1, ChronoUnit.DAYS),
						null,
						foreignScheduleId,
						List.of(
								new VoteOptionCreateRequest("연돈", null, null, null),
								new VoteOptionCreateRequest("제주바다", null, null, null)));

		assertThatThrownBy(() -> voteService.create(planId, creatorId, request))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.SCHEDULE_NOT_IN_PLAN);
	}

	@Test
	@DisplayName("투표 수정으로도 다른 계획의 일정에 연결할 수 없다")
	void updateVoteLinkedToScheduleOfAnotherPlan() {
		VoteResponse vote = voteService.create(planId, creatorId, createRequest());
		Long otherPlanId = fixture.createPlan(creatorId, "부산 당일치기");
		Long foreignScheduleId = fixture.createSchedule(otherPlanId, "남의 계획 일정");
		VoteUpdateRequest request = new VoteUpdateRequest(null, null, null, foreignScheduleId);

		assertThatThrownBy(() -> voteService.update(planId, id(vote.id()), creatorId, request))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.SCHEDULE_NOT_IN_PLAN);
	}

	@Test
	@DisplayName("선택지가 2개뿐이면 삭제할 수 없다")
	void deleteOptionBelowMinimum() {
		VoteResponse vote = voteService.create(planId, creatorId, createRequest());
		String optionId = vote.options().get(0).id();

		assertThatThrownBy(
						() -> voteService.deleteOption(planId, id(vote.id()), id(optionId), creatorId))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.OPTION_BELOW_MINIMUM);
	}

	@Test
	@DisplayName("선택지를 삭제하면 그 선택지에 걸린 참여 기록도 사라진다")
	void deleteOptionRemovesParticipation() {
		VoteResponse vote = voteService.create(planId, creatorId, createRequest());
		voteService.addOption(
				planId, id(vote.id()), creatorId, new VoteOptionCreateRequest("우도", null, null, null));
		String optionId = vote.options().get(0).id();
		voteParticipationService.participate(
				planId, id(vote.id()), participantId, new VoteParticipationRequest(id(optionId), null));

		voteService.deleteOption(planId, id(vote.id()), id(optionId), creatorId);

		VoteResponse found = voteService.findById(planId, id(vote.id()), participantId);
		assertThat(found.options()).hasSize(2);
		assertThat(found.participantCount()).isZero();
	}

	@Test
	@DisplayName("다른 투표의 선택지 ID로는 수정할 수 없다")
	void updateOptionOfAnotherVote() {
		VoteResponse first = voteService.create(planId, creatorId, createRequest());
		VoteResponse second = voteService.create(planId, creatorId, createRequest());
		String foreignOptionId = second.options().get(0).id();

		assertThatThrownBy(
						() ->
								voteService.updateOption(
										planId,
										id(first.id()),
										id(foreignOptionId),
										creatorId,
										new VoteOptionUpdateRequest("우도", null, null, null, false)))
				.isInstanceOf(VoteException.class)
				.extracting(exception -> ((VoteException) exception).getErrorCode())
				.isEqualTo(VoteErrorCode.OPTION_NOT_IN_VOTE);
	}

	@Test
	@DisplayName("투표를 삭제하면 선택지와 참여 기록도 함께 사라진다")
	void deleteVote() {
		VoteResponse vote = voteService.create(planId, creatorId, createRequest());
		voteParticipationService.participate(
				planId,
				id(vote.id()),
				participantId,
				new VoteParticipationRequest(id(vote.options().get(0).id()), null));

		voteService.delete(planId, id(vote.id()), creatorId);

		assertThat(voteRepository.findById(id(vote.id()))).isEmpty();
	}

	private VoteCreateRequest createRequest() {
		return new VoteCreateRequest(
				"첫날 어디 갈까요",
				"오전 일정 후보입니다",
				Instant.now().plus(1, ChronoUnit.DAYS),
				VoteType.SINGLE,
				null,
				List.of(
						new VoteOptionCreateRequest("성산일출봉", "제주 서귀포시 성산읍", "🌅", placeId),
						new VoteOptionCreateRequest("협재해수욕장", "제주 한림읍", "🏖️", null)));
	}

	private VoteCreateRequest minimalRequest() {
		return new VoteCreateRequest(
				"어디로 갈까요",
				null,
				Instant.now().plus(1, ChronoUnit.DAYS),
				null,
				null,
				List.of(
						new VoteOptionCreateRequest("성산일출봉", null, null, null),
						new VoteOptionCreateRequest("협재해수욕장", null, null, null)));
	}

	private static long id(String value) {
		return Long.parseLong(value);
	}
}
