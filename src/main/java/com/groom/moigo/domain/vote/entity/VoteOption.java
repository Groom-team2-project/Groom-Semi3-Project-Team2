package com.groom.moigo.domain.vote.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 투표 선택지. {@code V1__init_schema.sql}의 {@code vote_options} 테이블에 대응한다.
 *
 * <p>{@code content}는 ERD의 선택지.내용 컬럼이며 화면에 노출되는 후보 장소 이름을 담는다. 응답에서는 프론트엔드 계약에 맞춰 {@code placeName}으로
 * 내려간다.
 *
 * <p>{@code place_address}, {@code emoji}는 V2 마이그레이션으로 추가한 컬럼이다. 프론트엔드가 투표를 만들 때 장소 ID 대신 이름·주소·이모지를
 * 보내오고, 이모지는 {@code places} 테이블에도 둘 자리가 없어 선택지에 스냅샷으로 저장한다. 투표는 당시 후보가 무엇이었는지 남는 편이 맞기도 하다.
 */
@Entity
@Table(name = "vote_options")
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

	/** 저장된 장소 ID. 장소 도메인이 엔티티를 제공하기 전까지 식별자로만 들고 있다. */
	@Column(name = "place_id")
	private Long placeId;

	/** 선택지 내용. 후보 장소 이름이 들어간다. */
	@Column(name = "content", nullable = false, length = 200)
	private String content;

	/** 후보 장소 주소 스냅샷. 장소 검색 결과에 주소가 없으면 비어 있을 수 있다. */
	@Column(name = "place_address", length = 300)
	private String placeAddress;

	@Column(name = "emoji", nullable = false, length = 20)
	private String emoji;

	@Builder
	private VoteOption(Long placeId, String content, String placeAddress, String emoji) {
		this.placeId = placeId;
		this.content = content;
		this.placeAddress = placeAddress;
		this.emoji = normalizeEmoji(emoji);
	}

	void assignTo(Vote vote) {
		this.vote = vote;
	}

	/**
	 * null인 필드는 변경하지 않는다.
	 *
	 * <p>장소 연결 해제는 {@code clearPlace}로만 한다. {@code placeId}가 null인 것은 "생략"이지 "해제"가 아니다. 요청 DTO에서 생략과
	 * 명시적 null을 구분할 수 없기 때문이다.
	 */
	public void update(
			String content, String placeAddress, String emoji, Long placeId, boolean clearPlace) {
		if (content != null) {
			this.content = content;
		}
		if (placeAddress != null) {
			this.placeAddress = placeAddress;
		}
		if (emoji != null) {
			this.emoji = normalizeEmoji(emoji);
		}
		if (clearPlace) {
			this.placeId = null;
		} else if (placeId != null) {
			this.placeId = placeId;
		}
	}

	public boolean belongsTo(Long voteId) {
		return vote.getId().equals(voteId);
	}

	private static String normalizeEmoji(String emoji) {
		return emoji == null || emoji.isBlank() ? DEFAULT_EMOJI : emoji;
	}
}
