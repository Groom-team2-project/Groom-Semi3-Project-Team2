package com.groom.moigo.domain.vote.service;

import com.groom.moigo.domain.activity.dto.ActivityRecordCommand;
import com.groom.moigo.domain.activity.entity.ActivityActionType;
import com.groom.moigo.domain.activity.entity.ActivityTargetType;
import com.groom.moigo.domain.activity.service.ActivityLogService;
import com.groom.moigo.domain.vote.entity.Vote;
import com.groom.moigo.domain.vote.entity.VoteStatus;
import com.groom.moigo.domain.vote.repository.VoteRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 마감 일시가 지난 투표를 닫고 활동 이력을 남긴다.
 *
 * <p>조회할 때 {@code syncStatus}로 상태를 맞추고는 있지만 그건 응답을 바로잡을 뿐이라, 아무도 들여다보지 않으면 DB는 진행 중인 채로 남고 마감
 * 활동도 기록되지 않는다. 활동 기록 정책은 투표 마감을 공유 활동으로 남기라고 하므로 마감되는 순간을 잡아 줄 무언가가 필요하다.
 *
 * <p>NOTE 인스턴스를 여러 대로 늘리면 같은 투표를 두 대가 동시에 닫아 이력이 두 번 남을 수 있다. 그때는 잠금이나 분산 스케줄러가 필요하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoteDeadlineCloser {

	private final VoteRepository voteRepository;
	private final ActivityLogService activityLogService;

	/** 기본 1분마다 확인한다. 마감 시각과 이력이 남는 시각 사이의 오차가 이 간격만큼 생긴다. */
	@Scheduled(fixedDelayString = "${moigo.vote.deadline-closer.interval-ms:60000}")
	@Transactional
	public void closeExpiredVotes() {
		Instant now = Instant.now();
		List<Vote> expired =
				voteRepository.findByStatusAndEndDatetimeLessThanEqual(VoteStatus.OPEN, now);
		if (expired.isEmpty()) {
			return;
		}

		for (Vote vote : expired) {
			vote.close();
			activityLogService.record(
					new ActivityRecordCommand(
							vote.getPlanId(),
							// 시간이 되어 저절로 닫힌 것이라 수행한 사람이 없다.
							null,
							ActivityActionType.VOTE_CLOSED,
							ActivityTargetType.VOTE,
							vote.getId(),
							"'%s' 투표가 마감됐어요".formatted(vote.getTitle())));
		}
		log.info("마감 일시가 지난 투표 {}건을 닫았습니다.", expired.size());
	}
}
