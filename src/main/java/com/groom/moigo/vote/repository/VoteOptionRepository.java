package com.groom.moigo.vote.repository;

import com.groom.moigo.vote.entity.VoteOption;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteOptionRepository extends JpaRepository<VoteOption, Long> {

	List<VoteOption> findByVoteIdOrderByIdAsc(Long voteId);

	List<VoteOption> findByVoteIdAndIdIn(Long voteId, List<Long> optionIds);

	long countByVoteId(Long voteId);
}
