package com.groom.moigo.vote.entity;

import com.groom.moigo.auth.entity.Member;
import com.groom.moigo.plan.entity.Plan;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 투표.
 *
 * <p>하나의 계획(Plan)에 속하며 여러 개의 선택지(VoteOption)를 가진다. 종료 일시가 지나면 상태는 종료로 간주한다.
 *
 * <p>시각은 {@link Instant}로 다룬다. 프론트엔드가 {@code toISOString()}으로 UTC 기준 절대 시각을 보내오므로 서버 타임존에 흔들리지 않도록
 * 하기 위함이다.
 */
@Entity
@Table(
		name = "vote",
		indexes = {
			@Index(name = "idx_vote_plan_id", columnList = "plan_id"),
			@Index(name = "idx_vote_creator_id", columnList = "creator_id")
		})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vote {

	private static final int MIN_OPTION_COUNT = 2;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "vote_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "plan_id", nullable = false)
	private Plan plan;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "creator_id", nullable = false)
	private Member creator;

	@Column(name = "title", nullable = false, length = 100)
	private String title;

	@Column(name = "description", length = 500)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "vote_type", nullable = false, length = 20)
	private VoteType type;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private VoteStatus status;

	/** 마감 일시. null이면 생성자가 직접 종료할 때까지 진행한다. 응답에서는 {@code deadline}으로 내려간다. */
	@Column(name = "closes_at")
	private Instant closesAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@OneToMany(mappedBy = "vote", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<VoteOption> options = new ArrayList<>();

	@Builder
	private Vote(
			Plan plan, Member creator, String title, String description, VoteType type, Instant closesAt) {
		this.plan = plan;
		this.creator = creator;
		this.title = title;
		this.description = description;
		this.type = type == null ? VoteType.SINGLE : type;
		this.status = VoteStatus.OPEN;
		this.closesAt = closesAt;
	}

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public void addOption(VoteOption option) {
		options.add(option);
		option.assignTo(this);
	}

	/** 선택지는 최소 2개를 유지해야 하므로 그 이상일 때만 삭제할 수 있다. */
	public boolean canRemoveOption() {
		return options.size() > MIN_OPTION_COUNT;
	}

	public void update(String title, String description, Instant closesAt) {
		if (title != null) {
			this.title = title;
		}
		if (description != null) {
			this.description = description;
		}
		if (closesAt != null) {
			this.closesAt = closesAt;
		}
	}

	public void close() {
		this.status = VoteStatus.CLOSED;
	}

	/** 종료 일시가 지났는데 상태가 아직 진행이면 종료로 동기화한다. 상태가 바뀌었으면 true. */
	public boolean syncStatus(Instant now) {
		if (status == VoteStatus.OPEN && closesAt != null && !now.isBefore(closesAt)) {
			status = VoteStatus.CLOSED;
			return true;
		}
		return false;
	}

	public boolean isClosed(Instant now) {
		return status == VoteStatus.CLOSED || (closesAt != null && !now.isBefore(closesAt));
	}

	public boolean isCreatedBy(Long memberId) {
		return creator.getId().equals(memberId);
	}

	public boolean belongsToPlan(Long planId) {
		return plan.getId().equals(planId);
	}
}
