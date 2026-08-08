package com.groom.moigo.vote.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 투표 참여 요청. 이미 참여한 회원이 다시 요청하면 기존 선택을 덮어쓴다.
 *
 * @param optionIds 선택한 선택지 ID 목록. 단일 선택 투표는 1개만 허용
 */
public record VoteParticipationRequest(
		@NotEmpty(message = "선택지를 하나 이상 선택해야 합니다.") List<Long> optionIds) {}
