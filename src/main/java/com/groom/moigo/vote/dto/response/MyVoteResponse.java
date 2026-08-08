package com.groom.moigo.vote.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 내 투표 참여 내역 응답.
 *
 * @param voteId 투표 ID
 * @param memberId 회원 ID
 * @param selectedOptionIds 내가 선택한 선택지 ID 목록
 * @param participatedAt 최초 참여 일시
 */
public record MyVoteResponse(
		Long voteId, Long memberId, List<Long> selectedOptionIds, LocalDateTime participatedAt) {}
