package com.groom.moigo.domain.vote.repository;

import com.groom.moigo.domain.vote.entity.VoteOption;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteOptionRepository extends JpaRepository<VoteOption, Long> {

	List<VoteOption> findByVoteIdOrderByIdAsc(Long voteId);

	/** 투표 목록 응답을 조립할 때 선택지를 한 번에 가져와 투표 수만큼 질의하지 않도록 한다. */
	List<VoteOption> findByVoteIdInOrderByVoteIdAscIdAsc(List<Long> voteIds);

	List<VoteOption> findByVoteIdAndIdIn(Long voteId, List<Long> optionIds);

	long countByVoteId(Long voteId);
}
