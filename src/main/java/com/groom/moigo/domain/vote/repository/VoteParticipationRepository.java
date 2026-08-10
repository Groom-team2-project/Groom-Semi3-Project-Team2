package com.groom.moigo.domain.vote.repository;

import com.groom.moigo.domain.vote.entity.VoteParticipation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoteParticipationRepository extends JpaRepository<VoteParticipation, Long> {

	List<VoteParticipation> findByVoteIdAndUserIdOrderByIdAsc(Long voteId, Long userId);

	@Modifying(flushAutomatically = true)
	@Query("delete from VoteParticipation p where p.vote.id = :voteId and p.userId = :userId")
	int deleteByVoteIdAndUserId(@Param("voteId") Long voteId, @Param("userId") Long userId);

	/** 삭제된 참여 기록이 영속성 컨텍스트에 남아 선택지 삭제와 충돌하지 않도록 컨텍스트를 비운다. */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from VoteParticipation p where p.option.id = :optionId")
	int deleteByOptionId(@Param("optionId") Long optionId);

	/** 삭제된 참여 기록이 영속성 컨텍스트에 남아 투표 삭제와 충돌하지 않도록 컨텍스트를 비운다. */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from VoteParticipation p where p.vote.id = :voteId")
	int deleteByVoteId(@Param("voteId") Long voteId);

	@Query("select count(distinct p.userId) from VoteParticipation p where p.vote.id = :voteId")
	long countDistinctUserByVoteId(@Param("voteId") Long voteId);

	@Query(
			"""
			select p.option.id as optionId, count(p.id) as voteCount
			from VoteParticipation p
			where p.vote.id = :voteId
			group by p.option.id
			""")
	List<OptionVoteCount> countGroupedByOption(@Param("voteId") Long voteId);

	/** 투표 목록 응답용. 여러 투표의 선택지별 득표 수를 한 번에 집계한다. */
	@Query(
			"""
			select p.option.id as optionId, count(p.id) as voteCount
			from VoteParticipation p
			where p.vote.id in :voteIds
			group by p.option.id
			""")
	List<OptionVoteCount> countGroupedByOptionIn(@Param("voteIds") List<Long> voteIds);

	@Query(
			"""
			select p.vote.id as voteId, count(distinct p.userId) as participantCount
			from VoteParticipation p
			where p.vote.id in :voteIds
			group by p.vote.id
			""")
	List<VoteParticipantCount> countParticipantsByVoteIds(@Param("voteIds") List<Long> voteIds);

	/** 투표 목록 응답용. 여러 투표에 걸친 내 선택지 ID를 한 번에 가져온다. */
	@Query(
			"""
			select p.option.id
			from VoteParticipation p
			where p.vote.id in :voteIds and p.userId = :userId
			order by p.id asc
			""")
	List<Long> findSelectedOptionIds(
			@Param("voteIds") List<Long> voteIds, @Param("userId") Long userId);

	/** 선택지별 득표 수 집계 결과. */
	interface OptionVoteCount {
		Long getOptionId();

		long getVoteCount();
	}

	/** 투표별 참여 인원(중복 제거) 집계 결과. */
	interface VoteParticipantCount {
		Long getVoteId();

		long getParticipantCount();
	}
}
