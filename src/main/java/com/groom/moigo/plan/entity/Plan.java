package com.groom.moigo.plan.entity;

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
 * 계획 최소 엔티티.
 *
 * <p>TODO 계획·멤버·초대 도메인(담당: 주정현) 구현이 머지되면 이 클래스를 제거하고 해당 구현을 사용한다.
 * 투표가 어느 계획에 속하는지 표현하기 위한 임시 스텁이다.
 */
@Entity
@Table(name = "plan")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Plan {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "plan_id")
	private Long id;

	@Column(name = "title", nullable = false, length = 100)
	private String title;

	public Plan(String title) {
		this.title = title;
	}
}
