package com.groom.moigo.vote.service;

import com.groom.moigo.vote.dto.response.VoteOptionResponse;
import com.groom.moigo.vote.dto.response.VoteResponse;
import com.groom.moigo.vote.dto.response.VoteResultResponse;
import com.groom.moigo.vote.entity.Vote;
import com.groom.moigo.vote.entity.VoteOption;
import com.groom.moigo.vote.repository.VoteOptionRepository;
import com.groom.moigo.vote.repository.VoteParticipationRepository;
import com.groom.moigo.vote.repository.VoteParticipationRepository.OptionVoteCount;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 투표 조회 응답을 조립한다. 선택지별 득표 수와 요청자의 선택 여부를 한 번에 채워 준다. */
@Component
@RequiredArgsConstructor
class VoteResponseAssembler {

	private final VoteOptionRepository voteOptionRepository;
	private final VoteParticipationRepository voteParticipationRepository;

	VoteResponse toVoteResponse(Vote vote, Long memberId) {
		List<VoteOption> options = voteOptionRepository.findByVoteIdOrderByIdAsc(vote.getId());
		Map<Long, Long> voteCounts = voteCountsByOption(vote.getId());
		Set<Long> mySelections = selectedOptionIds(vote.getId(), memberId);

		List<VoteOptionResponse> optionResponses =
				options.stream()
						.map(
								option ->
										VoteOptionResponse.of(
												option,
												voteCounts.getOrDefault(option.getId(), 0L),
												mySelections.contains(option.getId())))
						.toList();

		long participantCount = voteParticipationRepository.countDistinctMemberByVoteId(vote.getId());
		return VoteResponse.of(vote, participantCount, !mySelections.isEmpty(), optionResponses);
	}

	VoteResultResponse toResultResponse(Vote vote) {
		List<VoteOption> options = voteOptionRepository.findByVoteIdOrderByIdAsc(vote.getId());
		Map<Long, Long> voteCounts = voteCountsByOption(vote.getId());
		long maxCount = voteCounts.values().stream().mapToLong(Long::longValue).max().orElse(0L);

		List<VoteResultResponse.OptionResult> results =
				options.stream()
						.map(option -> toOptionResult(option, voteCounts.getOrDefault(option.getId(), 0L), maxCount))
						.sorted(
								Comparator.comparingLong(VoteResultResponse.OptionResult::voteCount)
										.reversed()
										.thenComparing(VoteResultResponse.OptionResult::optionId))
						.toList();

		long participantCount = voteParticipationRepository.countDistinctMemberByVoteId(vote.getId());
		return VoteResultResponse.of(vote, participantCount, withPercentage(results));
	}

	VoteOptionResponse toOptionResponse(VoteOption option, Long memberId) {
		Map<Long, Long> voteCounts = voteCountsByOption(option.getVote().getId());
		Set<Long> mySelections = selectedOptionIds(option.getVote().getId(), memberId);
		return VoteOptionResponse.of(
				option,
				voteCounts.getOrDefault(option.getId(), 0L),
				mySelections.contains(option.getId()));
	}

	Map<Long, Long> voteCountsByOption(Long voteId) {
		Map<Long, Long> counts = new HashMap<>();
		for (OptionVoteCount row : voteParticipationRepository.countGroupedByOption(voteId)) {
			counts.put(row.getOptionId(), row.getVoteCount());
		}
		return counts;
	}

	Set<Long> selectedOptionIds(Long voteId, Long memberId) {
		if (memberId == null) {
			return Set.of();
		}
		return voteParticipationRepository.findByVoteIdAndMemberIdOrderByIdAsc(voteId, memberId).stream()
				.map(participation -> participation.getOption().getId())
				.collect(Collectors.toSet());
	}

	private VoteResultResponse.OptionResult toOptionResult(
			VoteOption option, long voteCount, long maxCount) {
		return new VoteResultResponse.OptionResult(
				option.getId(),
				option.getContent(),
				option.getPlace() == null ? null : option.getPlace().getId(),
				voteCount,
				0.0,
				maxCount > 0 && voteCount == maxCount);
	}

	private List<VoteResultResponse.OptionResult> withPercentage(
			List<VoteResultResponse.OptionResult> results) {
		long total = results.stream().mapToLong(VoteResultResponse.OptionResult::voteCount).sum();
		if (total == 0) {
			return results;
		}
		return results.stream()
				.map(
						result ->
								new VoteResultResponse.OptionResult(
										result.optionId(),
										result.content(),
										result.placeId(),
										result.voteCount(),
										Math.round(result.voteCount() * 1000.0 / total) / 10.0,
										result.winner()))
				.toList();
	}
}
