package com.groom.moigo.domain.place.repository;

import com.groom.moigo.domain.place.entity.PlaceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<PlaceEntity,Long> {
    Optional<PlaceEntity> findByPlaceIdAndDeletedAtIsNull(Long placeId);
    List<PlaceEntity> findAllByKakaoPlaceIdIn(Collection<String> kakaoPlaceIds);
    Page<PlaceEntity> findAllByDeletedAtIsNull(Pageable pageable);

    @Query("""
            select p from PlaceEntity p
            where p.deletedAt is null
              and (:keyword is null
                   or lower(p.name) like lower(concat('%', :keyword, '%'))
                   or lower(p.address) like lower(concat('%', :keyword, '%'))
                   or lower(p.roadAddress) like lower(concat('%', :keyword, '%')))
            """)
    Page<PlaceEntity> searchActive(@Param("keyword") String keyword, Pageable pageable);
}
