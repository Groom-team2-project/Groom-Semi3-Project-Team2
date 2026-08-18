package com.groom.moigo.domain.place.repository;

import com.groom.moigo.domain.place.entity.PlaceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<PlaceEntity,Long> {
    Optional<PlaceEntity> findByPlaceIdAndDeletedAtIsNull(Long placeId);
    List<PlaceEntity> findAllByKakaoPlaceIdIn(Collection<String> kakaoPlaceIds);
    Page<PlaceEntity> findAllByDeletedAtIsNull(Pageable pageable);
}
