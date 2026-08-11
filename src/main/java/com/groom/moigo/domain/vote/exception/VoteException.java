package com.groom.moigo.domain.vote.exception;

import lombok.Getter;

/** 투표 도메인 비즈니스 예외. */
@Getter
public class VoteException extends RuntimeException {

	private final transient VoteErrorCode errorCode;

	public VoteException(VoteErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public VoteException(VoteErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}
}
