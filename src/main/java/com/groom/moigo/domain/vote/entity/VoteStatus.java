package com.groom.moigo.domain.vote.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 투표 상태. ERD의 투표.상태(진행/종료). */
@Getter
@RequiredArgsConstructor
public enum VoteStatus {

	OPEN("진행"),
	CLOSED("종료");

	private final String description;
}
