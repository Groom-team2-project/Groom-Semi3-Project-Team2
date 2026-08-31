package com.groom.moigo.domain.place.repository;

import com.groom.moigo.domain.place.entity.PlaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlaceRepository extends JpaRepository<PlaceEntity, Long> {
    Optional<PlaceEntity> findByPlaceIdAndDeletedAtIsNull(Long placeId);
    Optional<PlaceEntity> findByKakaoPlaceId(String kakaoPlaceId);
}
