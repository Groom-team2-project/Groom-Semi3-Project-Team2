package com.groom.moigo.auth.entity;

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
 * 회원 최소 엔티티.
 *
 * <p>TODO 인증·회원 도메인(담당: 박선우) 구현이 머지되면 이 클래스를 제거하고 해당 구현을 사용한다.
 * 투표 도메인이 참조하는 회원ID/식별 정보만 담은 임시 스텁이다.
 */
@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "member_id")
	private Long id;

	@Column(name = "email", nullable = false, unique = true, length = 255)
	private String email;

	@Column(name = "nickname", nullable = false, unique = true, length = 50)
	private String nickname;

	@Column(name = "profile_image_url", length = 500)
	private String profileImageUrl;

	public Member(String email, String nickname, String profileImageUrl) {
		this.email = email;
		this.nickname = nickname;
		this.profileImageUrl = profileImageUrl;
	}
}
