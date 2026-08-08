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
 * <p>{@code content}는 ERD의 선택지.내용 컬럼이며 화면에 노출되는 후보 장소 이름을 담는다. 응답에서는 프론트엔드 계약에 맞춰 {@code placeName}으로
 * 내려간다.
 *
 * <p>프론트엔드는 카카오 장소 검색 결과를 저장하기 전에 투표를 만들기 때문에 {@code place} FK 없이 이름·주소·이모지만 전달한다. 그래서 주소와 이모지를
 * 선택지에 스냅샷으로 함께 저장한다. 장소를 먼저 저장한 뒤 {@code placeId}를 보내는 흐름도 그대로 지원한다(ERD상 장소ID NULL 허용).
 *
 * <p>NOTE {@code place_address}, {@code emoji}는 ERD의 VOTE_OPTIONS에 없는 추가 컬럼이다. 유지하기로 결정했다. 투표 목록·상세
 * 화면은 아직 둘 다 그리지 않지만(후보 이름만 노출), 프론트엔드가 투표 생성 시 두 값을 보내오고 {@code VoteOption} 타입에도 선언되어 있다. 특히
 * {@code emoji}는 옵셔널이 아닌 필수 필드라 응답에서 빠지면 화면 연동 시 타입이 깨진다. 받은 값을 버리지 않고 그대로 돌려주기 위해 둔다. ERD 반영
 * 여부는 팀 논의 필요.
 */
@Entity
@Table(
		name = "vote_options",
		indexes = {@Index(name = "idx_vote_options_vote_id", columnList = "vote_id")})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoteOption {

	/** 이모지를 받지 못했을 때 쓰는 기본 핀 이모지. */
	private static final String DEFAULT_EMOJI = "📍";

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

	/** 선택지 내용. 후보 장소 이름이 들어간다. */
	@Column(name = "content", nullable = false, length = 200)
	private String content;

	/** 후보 장소 주소 스냅샷. 장소 검색 결과에 주소가 없으면 비어 있을 수 있다. */
	@Column(name = "place_address", length = 300)
	private String placeAddress;

	@Column(name = "emoji", nullable = false, length = 20)
	private String emoji;

	@Builder
	private VoteOption(Place place, String content, String placeAddress, String emoji) {
		this.place = place;
		this.content = content;
		this.placeAddress = placeAddress;
		this.emoji = normalizeEmoji(emoji);
	}

	void assignTo(Vote vote) {
		this.vote = vote;
	}

	/** null인 필드는 변경하지 않는다. 다만 {@code place}는 null을 보내면 장소 연결이 해제된다. */
	public void update(String content, String placeAddress, String emoji, Place place) {
		if (content != null) {
			this.content = content;
		}
		if (placeAddress != null) {
			this.placeAddress = placeAddress;
		}
		if (emoji != null) {
			this.emoji = normalizeEmoji(emoji);
		}
		this.place = place;
	}

	public boolean belongsTo(Long voteId) {
		return vote.getId().equals(voteId);
	}

	private static String normalizeEmoji(String emoji) {
		return emoji == null || emoji.isBlank() ? DEFAULT_EMOJI : emoji;
	}
}
