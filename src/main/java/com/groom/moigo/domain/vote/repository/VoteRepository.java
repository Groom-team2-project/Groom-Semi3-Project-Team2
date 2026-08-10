package com.groom.moigo.domain.vote.repository;

import com.groom.moigo.domain.vote.entity.Vote;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRepository extends JpaRepository<Vote, Long> {

	List<Vote> findByPlanIdOrderByCreatedAtDesc(Long planId);
}
