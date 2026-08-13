package com.groom.moigo.domain.place.service;

import com.groom.moigo.domain.place.dto.PlaceListResponse;
import com.groom.moigo.domain.place.dto.PlacePageResponse;
import com.groom.moigo.domain.place.dto.PlaceResponse;
import com.groom.moigo.domain.place.entity.PlaceEntity;
import com.groom.moigo.domain.place.repository.PlaceRepository;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;


@Service
@RequiredArgsConstructor
public class PlaceService {
    private final PlaceRepository placeRepository;

    public PlaceResponse getPlace(Long placeId) {
        PlaceEntity place = placeRepository
                .findByPlaceIdAndDeletedAtIsNull(placeId)
                .orElseThrow(()-> new IllegalArgumentException("장소를 찾을 수 없습니다."));
        return PlaceResponse.from(place);
    }
    public PlaceListResponse getPlaces(int page, int size) {
        Pageable pageable = PageRequest.of(
                page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PlaceEntity> result = placeRepository.findAllByDeletedAtIsNull(pageable);
        return PlaceListResponse.from(result);
    }

    public PlacePageResponse searchPlaces(
            String query,
            int page,
            int size,
            String sort,
            BigDecimal latitude,
            BigDecimal longitude,
            Integer radius
    ){

    }
}