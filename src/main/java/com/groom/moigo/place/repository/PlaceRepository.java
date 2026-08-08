package com.groom.moigo.place.repository;

import com.groom.moigo.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * TODO 일정·장소·카카오 API 도메인(담당: 박소빈) 구현이 머지되면 이 인터페이스를 제거하고 해당 구현을 사용한다.
 *
 * <p>투표 선택지가 참조하는 장소 존재를 검증하기 위한 임시 스텁이다.
 */
public interface PlaceRepository extends JpaRepository<Place, Long> {}
