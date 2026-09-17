package com.groom.moigo.domain.vote.dto.request;

import java.util.List;
import java.util.stream.Stream;

/**
 * 투표 참여 요청. 이미 참여한 회원이 다시 요청하면 기존 선택을 덮어쓴다.
 *
 * <p>프론트엔드 투표 상세 화면은 후보를 하나만 눌러 투표하므로 {@code optionId} 하나만 보낸다. 복수 선택 투표를 위해 {@code optionIds}도 함께
 * 받는다. 둘 다 오면 합쳐서 처리한다.
 *
 * @param optionId 선택한 선택지 ID(단일 선택)
 * @param optionIds 선택한 선택지 ID 목록(복수 선택)
 */
public record VoteParticipationRequest(Long optionId, List<Long> optionIds) {

	/** 두 필드를 합쳐 실제로 고른 선택지 목록으로 만든다. 비어 있으면 서비스에서 검증 예외를 던진다. */
	public List<Long> selectedOptionIds() {
		if (optionIds == null || optionIds.isEmpty()) {
			return optionId == null ? List.of() : List.of(optionId);
		}
		if (optionId == null || optionIds.contains(optionId)) {
			return optionIds;
		}
		return Stream.concat(Stream.of(optionId), optionIds.stream()).toList();
	}
}
