package com.groom.moigo.vote.entity;

import com.groom.moigo.auth.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 투표 참여.
 *
 * <p>회원이 선택한 선택지 하나당 한 행이 생긴다. 복수 선택 투표에서는 한 회원이 여러 행을 가진다. 같은 회원이 같은 선택지를 중복으로 고를 수 없도록 유니크
 * 제약을 둔다.
 */
@Entity
@Table(
		name = "vote_participation",
		uniqueConstraints = {
			@UniqueConstraint(
					name = "uk_vote_participation_option_member",
					columnNames = {"option_id", "member_id"})
		},
		indexes = {
			@Index(name = "idx_vote_participation_vote_member", columnList = "vote_id, member_id"),
			@Index(name = "idx_vote_participation_option_id", columnList = "option_id")
		})
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

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(name = "participated_at", nullable = false, updatable = false)
	private LocalDateTime participatedAt;

	@Builder
	private VoteParticipation(Vote vote, VoteOption option, Member member) {
		this.vote = vote;
		this.option = option;
		this.member = member;
	}

	@PrePersist
	void prePersist() {
		if (participatedAt == null) {
			participatedAt = LocalDateTime.now();
		}
	}
}
