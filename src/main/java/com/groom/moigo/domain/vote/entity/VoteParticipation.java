package com.groom.moigo.domain.vote.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 투표 참여. {@code V1__init_schema.sql}의 {@code vote_participants} 테이블에 대응한다.
 *
 * <p>회원이 선택한 선택지 하나당 한 행이 생긴다. 복수 선택 투표에서는 한 회원이 여러 행을 가진다. 같은 회원이 같은 선택지를 중복으로 고를 수 없도록 하는 유니크
 * 제약({@code uk_vote_participants_vote_user_option})은 마이그레이션에 있다.
 */
@Entity
@Table(name = "vote_participants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoteParticipation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "participation_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "vote_id", nullable = false)
	private Vote vote;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "option_id", nullable = false)
	private VoteOption option;

	/** 투표에 참여한 회원. ERD의 참가자. */
	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "participated_at", nullable = false, updatable = false)
	private Instant participatedAt;

	@Builder
	private VoteParticipation(Vote vote, VoteOption option, Long userId) {
		this.vote = vote;
		this.option = option;
		this.userId = userId;
	}

	@PrePersist
	void prePersist() {
		if (participatedAt == null) {
			participatedAt = Instant.now();
		}
	}
}
