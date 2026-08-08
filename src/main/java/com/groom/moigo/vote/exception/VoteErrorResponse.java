package com.groom.moigo.vote.exception;

/** 투표 도메인 에러 응답 본문. */
public record VoteErrorResponse(String code, String message) {

	public static VoteErrorResponse of(VoteErrorCode errorCode, String message) {
		return new VoteErrorResponse(errorCode.name(), message);
	}
}
