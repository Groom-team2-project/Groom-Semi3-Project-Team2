package com.groom.moigo.domain.vote.repository;

import com.groom.moigo.domain.vote.entity.Vote;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoteRepository extends JpaRepository<Vote, Long> {

	List<Vote> findByPlanIdOrderByCreatedAtDesc(Long planId);

	/**
	 * 투표 참여용 조회. 투표 행에 잠금을 걸어 같은 회원의 동시 요청을 줄 세운다.
	 *
	 * <p>참여는 기존 선택을 지우고 새로 넣는 두 단계라, 잠금이 없으면 서로 다른 선택지로 들어온 동시 요청이 둘 다 성공해 단일 선택 투표인데도 한 회원이 두
	 * 표를 갖게 된다. 유니크 제약이 {@code (vote_id, user_id, option_id)}라 이 경우를 막지 못한다.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select v from Vote v where v.id = :voteId")
	Optional<Vote> findByIdForUpdate(@Param("voteId") Long voteId);
}
