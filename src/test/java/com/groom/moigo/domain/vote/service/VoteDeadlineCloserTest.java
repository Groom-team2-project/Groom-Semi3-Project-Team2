package com.groom.moigo.domain.vote.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.groom.moigo.domain.activity.dto.ActivityRecordCommand;
import com.groom.moigo.domain.activity.entity.ActivityActionType;
import com.groom.moigo.domain.vote.dto.request.VoteCreateRequest;
import com.groom.moigo.domain.vote.dto.request.VoteOptionCreateRequest;
import com.groom.moigo.domain.vote.dto.response.VoteResponse;
import com.groom.moigo.domain.vote.entity.VoteStatus;
import com.groom.moigo.domain.vote.repository.VoteRepository;
import com.groom.moigo.domain.vote.support.VoteTestFixture;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

// 활동 기록 검증은 발행 이벤트 기준(롤백 테스트에서는 커밋 후 저장이 미실행, 정책서 7.5절)
@SpringBootTest
@RecordApplicationEvents
@Transactional
class VoteDeadlineCloserTest {

	@Autowired private VoteService voteService;
	@Autowired private VoteDeadlineCloser voteDeadlineCloser;
	@Autowired private VoteRepository voteRepository;
	@Autowired private ApplicationEvents events;
	@Autowired private VoteTestFixture fixture;

	private Long planId;
	private Long creatorId;

	@BeforeEach
	void setUp() {
		creatorId = fixture.createUser("생성자");
		planId = fixture.createPlan(creatorId, "제주도 3박 4일");
	}

	@Test
	@DisplayName("마감 일시가 지난 투표를 닫고 마감 활동을 남긴다")
	void closesExpiredVoteAndRecordsActivity() {
		VoteResponse vote = voteService.create(planId, creatorId, request());
		long voteId = Long.parseLong(vote.id());
		expire(voteId);

		voteDeadlineCloser.closeExpiredVotes();

		assertThat(voteRepository.findById(voteId).orElseThrow().getStatus())
				.isEqualTo(VoteStatus.CLOSED);
		assertThat(events.stream(ActivityRecordCommand.class))
				.filteredOn(command -> command.targetId().equals(voteId))
				.filteredOn(command -> command.actionType() == ActivityActionType.VOTE_CLOSED)
				.singleElement()
				.satisfies(
						command -> {
							assertThat(command.planId()).isEqualTo(planId);
							// 시간이 되어 저절로 닫힌 것이라 수행한 사람이 없다.
							assertThat(command.userId()).isNull();
							assertThat(command.summary()).isEqualTo("'첫날 어디 갈까요' 투표가 마감됐어요");
						});
	}

	@Test
	@DisplayName("이미 닫힌 투표는 다시 닫지 않아 마감 활동이 두 번 남지 않는다")
	void doesNotCloseTwice() {
		VoteResponse vote = voteService.create(planId, creatorId, request());
		long voteId = Long.parseLong(vote.id());
		expire(voteId);

		voteDeadlineCloser.closeExpiredVotes();
		voteDeadlineCloser.closeExpiredVotes();

		assertThat(events.stream(ActivityRecordCommand.class))
				.filteredOn(command -> command.targetId().equals(voteId))
				.filteredOn(command -> command.actionType() == ActivityActionType.VOTE_CLOSED)
				.hasSize(1);
	}

	@Test
	@DisplayName("마감 일시가 남은 투표는 건드리지 않는다")
	void leavesOpenVoteAlone() {
		VoteResponse vote = voteService.create(planId, creatorId, request());
		long voteId = Long.parseLong(vote.id());

		voteDeadlineCloser.closeExpiredVotes();

		assertThat(voteRepository.findById(voteId).orElseThrow().getStatus())
				.isEqualTo(VoteStatus.OPEN);
		assertThat(events.stream(ActivityRecordCommand.class))
				.filteredOn(command -> command.targetId().equals(voteId))
				.noneMatch(command -> command.actionType() == ActivityActionType.VOTE_CLOSED);
	}

	/** 마감 일시를 과거로 돌려 스케줄러가 집어갈 상태로 만든다. */
	private void expire(long voteId) {
		voteRepository
				.findById(voteId)
				.orElseThrow()
				.update(null, null, Instant.now().minus(1, ChronoUnit.MINUTES));
	}

	private VoteCreateRequest request() {
		return new VoteCreateRequest(
				"첫날 어디 갈까요",
				null,
				Instant.now().plus(1, ChronoUnit.DAYS),
				null,
				null,
				List.of(
						new VoteOptionCreateRequest("성산일출봉", null, null, null),
						new VoteOptionCreateRequest("협재해수욕장", null, null, null)));
	}
}
