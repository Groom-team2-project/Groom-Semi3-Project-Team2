package com.groom.moigo.vote.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 투표 도메인 에러 코드. */
@Getter
@RequiredArgsConstructor
public enum VoteErrorCode {

	VOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "투표를 찾을 수 없습니다."),
	VOTE_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "투표 선택지를 찾을 수 없습니다."),
	VOTE_PARTICIPATION_NOT_FOUND(HttpStatus.NOT_FOUND, "참여한 투표 내역이 없습니다."),
	PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "계획을 찾을 수 없습니다."),
	MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
	PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다."),

	VOTE_ALREADY_CLOSED(HttpStatus.CONFLICT, "이미 종료된 투표입니다."),
	VOTE_NOT_IN_PLAN(HttpStatus.BAD_REQUEST, "해당 계획에 속하지 않은 투표입니다."),
	OPTION_NOT_IN_VOTE(HttpStatus.BAD_REQUEST, "해당 투표에 속하지 않은 선택지입니다."),
	OPTION_NOT_SELECTED(HttpStatus.BAD_REQUEST, "선택지를 하나 이상 선택해야 합니다."),
	SINGLE_CHOICE_ONLY(HttpStatus.BAD_REQUEST, "단일 선택 투표는 선택지를 하나만 고를 수 있습니다."),
	DUPLICATED_OPTION_SELECTED(HttpStatus.BAD_REQUEST, "같은 선택지를 중복으로 선택할 수 없습니다."),
	INVALID_DEADLINE(HttpStatus.BAD_REQUEST, "마감 일시는 현재 시각 이후여야 합니다."),
	OPTION_BELOW_MINIMUM(HttpStatus.BAD_REQUEST, "투표 선택지는 최소 2개 이상이어야 합니다."),

	NOT_VOTE_CREATOR(HttpStatus.FORBIDDEN, "투표 생성자만 수행할 수 있는 작업입니다.");

	private final HttpStatus status;
	private final String message;
}
