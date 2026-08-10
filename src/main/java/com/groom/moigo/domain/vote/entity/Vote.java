package com.groom.moigo.domain.vote.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
 * 투표. {@code V1__init_schema.sql}의 {@code votes} 테이블에 대응한다.
 *
 * <p>{@code plan_id}, {@code user_id}, {@code schedule_id}는 연관관계 대신 식별자로만 들고 있다. 계획·일정 도메인이 아직
 * 엔티티를 제공하지 않아 투표 도메인이 임의로 엔티티를 만들지 않기 위함이다. 참조 무결성은 마이그레이션의 FK 제약이 보장한다.
 *
 * <p>시각은 {@link Instant}로 다룬다. 프론트엔드가 {@code toISOString()}으로 UTC 기준 절대 시각을 보내오므로 서버 타임존에 흔들리지 않도록
 * 하기 위함이다.
 */
@Entity
@Table(name = "votes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vote {

	private static final int MIN_OPTION_COUNT = 2;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "vote_id")
	private Long id;

	@Column(name = "plan_id", nullable = false)
	private Long planId;

	/** 투표를 만든 회원. ERD의 VOTES.user_id(생성자). */
	@Column(name = "user_id", nullable = false)
	private Long userId;

	/** 이 투표가 채우려는 일정 자리. 일정과 무관한 투표면 null. */
	@Column(name = "schedule_id")
	private Long scheduleId;

	@Column(name = "title", nullable = false, length = 200)
	private String title;

	@Column(name = "description", length = 1000)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 20)
	private VoteType type;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private VoteStatus status;

	/** 마감 일시. 응답에서는 {@code deadline}으로 내려간다. */
	@Column(name = "end_datetime", nullable = false)
	private Instant endDatetime;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@OneToMany(mappedBy = "vote", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<VoteOption> options = new ArrayList<>();

	@Builder
	private Vote(
			Long planId,
			Long userId,
			Long scheduleId,
			String title,
			String description,
			VoteType type,
			Instant endDatetime) {
		this.planId = planId;
		this.userId = userId;
		this.scheduleId = scheduleId;
		this.title = title;
		this.description = description;
		this.type = type == null ? VoteType.SINGLE : type;
		this.status = VoteStatus.OPEN;
		this.endDatetime = endDatetime;
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

	public void update(String title, String description, Instant endDatetime) {
		if (title != null) {
			this.title = title;
		}
		if (description != null) {
			this.description = description;
		}
		if (endDatetime != null) {
			this.endDatetime = endDatetime;
		}
	}

	public void linkTo(Long scheduleId) {
		this.scheduleId = scheduleId;
	}

	public void close() {
		this.status = VoteStatus.CLOSED;
	}

	/** 마감 일시가 지났는데 상태가 아직 진행이면 마감으로 동기화한다. 상태가 바뀌었으면 true. */
	public boolean syncStatus(Instant now) {
		if (status == VoteStatus.OPEN && !now.isBefore(endDatetime)) {
			status = VoteStatus.CLOSED;
			return true;
		}
		return false;
	}

	public boolean isClosed(Instant now) {
		return status == VoteStatus.CLOSED || !now.isBefore(endDatetime);
	}

	public boolean isCreatedBy(Long userId) {
		return this.userId.equals(userId);
	}

	public boolean belongsToPlan(Long planId) {
		return this.planId.equals(planId);
	}
}
