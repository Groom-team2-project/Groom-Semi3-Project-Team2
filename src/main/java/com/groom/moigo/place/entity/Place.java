package com.groom.moigo.place.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 장소 최소 엔티티.
 *
 * <p>TODO 일정·장소·카카오 API 도메인(담당: 박소빈) 구현이 머지되면 이 클래스를 제거하고 해당 구현을 사용한다.
 * 투표 선택지가 특정 장소를 가리킬 수 있도록 하기 위한 임시 스텁이다.
 */
@Entity
@Table(name = "place")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "place_id")
	private Long id;

	@Column(name = "kakao_place_id", nullable = false, unique = true, length = 50)
	private String kakaoPlaceId;

	@Column(name = "name", nullable = false, length = 200)
	private String name;

	public Place(String kakaoPlaceId, String name) {
		this.kakaoPlaceId = kakaoPlaceId;
		this.name = name;
	}
}
