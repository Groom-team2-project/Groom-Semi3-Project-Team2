package com.groom.moigo.domain.vote.service;

import com.groom.moigo.domain.vote.dto.response.VoteOptionResponse;
import com.groom.moigo.domain.vote.dto.response.VoteResponse;
import com.groom.moigo.domain.vote.dto.response.VoteResultResponse;
import com.groom.moigo.domain.vote.entity.Vote;
import com.groom.moigo.domain.vote.entity.VoteOption;
import com.groom.moigo.domain.vote.entity.VoteStatus;
import com.groom.moigo.domain.vote.repository.VoteOptionRepository;
import com.groom.moigo.domain.vote.repository.VoteParticipationRepository;
import com.groom.moigo.domain.vote.repository.VoteParticipationRepository.OptionVoteCount;
import com.groom.moigo.domain.vote.repository.VoteParticipationRepository.VoteParticipantCount;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

	VoteResponse toVoteResponse(Vote vote, Long userId) {
		List<VoteOption> options = voteOptionRepository.findByVoteIdOrderByIdAsc(vote.getId());
		Map<Long, Long> voteCounts = voteCountsByOption(vote.getId());
		Set<Long> mySelections = selectedOptionIds(vote.getId(), userId);
		long participantCount = voteParticipationRepository.countDistinctUserByVoteId(vote.getId());

		return build(vote, options, voteCounts, mySelections, participantCount);
	}

	/**
	 * 투표 목록을 한 번에 조립한다.
	 *
	 * <p>선택지·득표 수·참여 인원·내 선택을 투표 개수와 무관하게 각각 한 번씩만 질의해 N+1을 피한다.
	 */
	List<VoteResponse> toVoteResponses(List<Vote> votes, Long userId) {
		if (votes.isEmpty()) {
			return List.of();
		}

		List<Long> voteIds = votes.stream().map(Vote::getId).toList();
		Map<Long, List<VoteOption>> optionsByVote = optionsByVote(voteIds);
		Map<Long, Long> voteCounts = toVoteCountMap(voteParticipationRepository.countGroupedByOptionIn(voteIds));
		Map<Long, Long> participantCounts = participantCountsByVote(voteIds);
		Set<Long> mySelections = selectedOptionIdsIn(voteIds, userId);

		return votes.stream()
				.map(
						vote ->
								build(
										vote,
										optionsByVote.getOrDefault(vote.getId(), List.of()),
										voteCounts,
										mySelections,
										participantCounts.getOrDefault(vote.getId(), 0L)))
				.toList();
	}

	VoteResultResponse toResultResponse(Vote vote) {
		List<VoteOption> options = voteOptionRepository.findByVoteIdOrderByIdAsc(vote.getId());
		Map<Long, Long> voteCounts = voteCountsByOption(vote.getId());
		long maxCount = maxVoteCount(options, voteCounts);

		List<VoteResultResponse.OptionResult> results =
				options.stream()
						.map(option -> toOptionResult(option, voteCounts.getOrDefault(option.getId(), 0L), maxCount))
						.sorted(
								Comparator.comparingLong(VoteResultResponse.OptionResult::voteCount)
										.reversed()
										.thenComparing(VoteResultResponse.OptionResult::optionId))
						.toList();

		long participantCount = voteParticipationRepository.countDistinctUserByVoteId(vote.getId());
		return VoteResultResponse.of(
				vote, participantCount, summarize(options, voteCounts), withPercentage(results));
	}

	VoteOptionResponse toOptionResponse(VoteOption option, Long userId) {
		Long voteId = option.getVote().getId();
		Map<Long, Long> voteCounts = voteCountsByOption(voteId);
		Set<Long> mySelections = selectedOptionIds(voteId, userId);
		return VoteOptionResponse.of(
				option, voteCounts.getOrDefault(option.getId(), 0L), mySelections.contains(option.getId()));
	}

	Set<Long> selectedOptionIds(Long voteId, Long userId) {
		if (userId == null) {
			return Set.of();
		}
		return voteParticipationRepository.findByVoteIdAndUserIdOrderByIdAsc(voteId, userId).stream()
				.map(participation -> participation.getOption().getId())
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private VoteResponse build(
			Vote vote,
			List<VoteOption> options,
			Map<Long, Long> voteCounts,
			Set<Long> mySelections,
			long participantCount) {
		List<VoteOptionResponse> optionResponses =
				options.stream()
						.map(
								option ->
										VoteOptionResponse.of(
												option,
												voteCounts.getOrDefault(option.getId(), 0L),
												mySelections.contains(option.getId())))
						.toList();

		// 선택지 순서를 그대로 유지해야 화면에서 후보 순서가 흔들리지 않는다.
		List<String> myOptionIds =
				options.stream()
						.map(VoteOption::getId)
						.filter(mySelections::contains)
						.map(String::valueOf)
						.toList();

		String resultSummary =
				vote.getStatus() == VoteStatus.CLOSED ? summarize(options, voteCounts) : null;

		return VoteResponse.of(vote, optionResponses, myOptionIds, participantCount, resultSummary);
	}

	/**
	 * 마감된 투표의 결과 요약 문구를 만든다.
	 *
	 * <p>예) {@code "씨원리조트 3표 · 확정"}, 동점이면 {@code "씨원리조트 외 1곳 3표 동률"}.
	 */
	private String summarize(List<VoteOption> options, Map<Long, Long> voteCounts) {
		long maxCount = maxVoteCount(options, voteCounts);
		if (maxCount == 0) {
			return "투표 없이 마감되었어요";
		}

		List<VoteOption> winners =
				options.stream()
						.filter(option -> voteCounts.getOrDefault(option.getId(), 0L) == maxCount)
						.toList();
		String topName = winners.get(0).getContent();
		if (winners.size() == 1) {
			return "%s %d표 · 확정".formatted(topName, maxCount);
		}
		return "%s 외 %d곳 %d표 동률".formatted(topName, winners.size() - 1, maxCount);
	}

	private long maxVoteCount(List<VoteOption> options, Map<Long, Long> voteCounts) {
		return options.stream()
				.mapToLong(option -> voteCounts.getOrDefault(option.getId(), 0L))
				.max()
				.orElse(0L);
	}

	private Map<Long, List<VoteOption>> optionsByVote(List<Long> voteIds) {
		Map<Long, List<VoteOption>> grouped = new LinkedHashMap<>();
		for (VoteOption option : voteOptionRepository.findByVoteIdInOrderByVoteIdAscIdAsc(voteIds)) {
			grouped.computeIfAbsent(option.getVote().getId(), key -> new ArrayList<>()).add(option);
		}
		return grouped;
	}

	private Map<Long, Long> voteCountsByOption(Long voteId) {
		return toVoteCountMap(voteParticipationRepository.countGroupedByOption(voteId));
	}

	private Map<Long, Long> toVoteCountMap(List<OptionVoteCount> rows) {
		Map<Long, Long> counts = new HashMap<>();
		for (OptionVoteCount row : rows) {
			counts.put(row.getOptionId(), row.getVoteCount());
		}
		return counts;
	}

	private Map<Long, Long> participantCountsByVote(List<Long> voteIds) {
		Map<Long, Long> counts = new HashMap<>();
		for (VoteParticipantCount row :
				voteParticipationRepository.countParticipantsByVoteIds(voteIds)) {
			counts.put(row.getVoteId(), row.getParticipantCount());
		}
		return counts;
	}

	private Set<Long> selectedOptionIdsIn(List<Long> voteIds, Long userId) {
		if (userId == null) {
			return Set.of();
		}
		return Set.copyOf(voteParticipationRepository.findSelectedOptionIds(voteIds, userId));
	}

	private VoteResultResponse.OptionResult toOptionResult(
			VoteOption option, long voteCount, long maxCount) {
		return new VoteResultResponse.OptionResult(
				String.valueOf(option.getId()),
				option.getContent(),
				option.getPlaceAddress(),
				option.getEmoji(),
				option.getPlaceId() == null ? null : String.valueOf(option.getPlaceId()),
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
										result.placeName(),
										result.placeAddress(),
										result.emoji(),
										result.placeId(),
										result.voteCount(),
										Math.round(result.voteCount() * 1000.0 / total) / 10.0,
										result.winner()))
				.toList();
	}
}
