package com.groom.moigo.domain.place.repository;

import com.groom.moigo.domain.place.entity.PlaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<PlaceEntity, Long> {
}
