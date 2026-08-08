package com.groom.moigo.vote.entity;

import com.groom.moigo.place.entity.Place;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 투표 선택지.
 *
 * <p>장소를 후보로 올리는 투표라면 {@code place}가 채워지고, 일반 텍스트 선택지라면 비어 있을 수 있다(ERD상 장소ID NULL 허용).
 */
@Entity
@Table(
		name = "vote_option",
		indexes = {@Index(name = "idx_vote_option_vote_id", columnList = "vote_id")})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoteOption {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "option_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "vote_id", nullable = false)
	private Vote vote;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "place_id")
	private Place place;

	@Column(name = "content", nullable = false, length = 200)
	private String content;

	@Builder
	private VoteOption(Place place, String content) {
		this.place = place;
		this.content = content;
	}

	void assignTo(Vote vote) {
		this.vote = vote;
	}

	public void update(String content, Place place) {
		if (content != null) {
			this.content = content;
		}
		this.place = place;
	}

	public boolean belongsTo(Long voteId) {
		return vote.getId().equals(voteId);
	}
}
