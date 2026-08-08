package com.groom.moigo.vote.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 투표 방식. ERD의 투표.방식(단일/복수 선택). */
@Getter
@RequiredArgsConstructor
public enum VoteType {

	SINGLE("단일 선택"),
	MULTIPLE("복수 선택");

	private final String description;

	public boolean allowsMultipleSelection() {
		return this == MULTIPLE;
	}
}
