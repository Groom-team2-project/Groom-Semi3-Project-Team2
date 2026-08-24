package com.groom.moigo.domain.place2.repository;

import com.groom.moigo.domain.place2.entity.PlaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<PlaceEntity, Long> {
}
