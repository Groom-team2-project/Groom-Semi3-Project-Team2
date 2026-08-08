package com.groom.moigo.auth.repository;

import com.groom.moigo.auth.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * TODO 인증·회원 도메인(담당: 박선우) 구현이 머지되면 이 인터페이스를 제거하고 해당 구현을 사용한다.
 *
 * <p>투표 도메인이 회원 존재를 검증하기 위한 임시 스텁이다.
 */
public interface MemberRepository extends JpaRepository<Member, Long> {}
